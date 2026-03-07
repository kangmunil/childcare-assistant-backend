package com.childcare.domain.ai.service;

import com.childcare.domain.ai.dto.AiChatRequest;
import com.childcare.domain.ai.dto.AiChatClarification;
import com.childcare.domain.ai.dto.AiChatCitation;
import com.childcare.domain.ai.dto.AiChatFeedbackRequest;
import com.childcare.domain.ai.dto.AiChatMeta;
import com.childcare.domain.ai.dto.AiChatQuickAction;
import com.childcare.domain.ai.dto.AiChatResponse;
import com.childcare.domain.profile.service.ChildProfilePromptService;
import com.childcare.global.exception.AiException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.UUID;

@Slf4j
@Service
public class AiService {

    private static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";
    private static final Pattern PROFILE_CONTEXT_CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\t]]");
    private static final Pattern PROFILE_CONTEXT_MULTI_SPACE_PATTERN = Pattern.compile("[ ]{2,}");
    private static final Set<String> SUPPORTED_INTENTS = Set.of(
            "AUTO",
            "GROWTH_CHECK",
            "SLEEP",
            "FEEDING",
            "DEVELOPMENT",
            "VACCINATION",
            "ROUTINE",
            "MEDICAL",
            "ALLERGY"
    );
    private static final List<String> FALLBACK_PROFILE_DOMAINS = List.of(
            "growth",
            "sleep",
            "feeding",
            "vaccination",
            "development",
            "routine",
            "medical",
            "allergy",
            "safety"
    );
    private static final Set<String> PROFILE_DOMAIN_SET = new LinkedHashSet<>(FALLBACK_PROFILE_DOMAINS);

    private final WebClient aiWebClient;
    private final ChildProfilePromptService childProfilePromptService;
    private final String aiInternalToken;

    @Value("${ai.chat-meta.enabled:false}")
    private boolean aiChatMetaEnabled = false;

    @Value("${ai.chat-meta.rollout-percent:0}")
    private int aiChatMetaRolloutPercent = 0;

    @Value("${ai.chat-timeout-seconds:35}")
    private int aiChatTimeoutSeconds = 35;

    public AiService(
            WebClient webClient,
            @Value("${ai.service-url}") String aiServiceUrl,
            @Value("${ai.internal-token:}") String aiInternalToken,
            ChildProfilePromptService childProfilePromptService
    ) {
        this.aiWebClient = webClient.mutate()
                .baseUrl(aiServiceUrl)
                .build();
        this.childProfilePromptService = childProfilePromptService;
        this.aiInternalToken = aiInternalToken;
    }

    public AiChatResponse chat(AiChatRequest request, String requestId, UUID memberId) {
        long startedAt = System.nanoTime();
        int messageLength = request.getMessage() == null ? 0 : request.getMessage().length();
        String contextMode = request.getEffectiveContextMode();
        request.setContextMode(contextMode);
        boolean isManualMode = "MANUAL".equals(contextMode);
        String sanitizedProfileContext = sanitizeProfileContext(request.getProfileContext());
        boolean hasManualContext = StringUtils.hasText(sanitizedProfileContext);
        String normalizedIntentHint = resolveIntentHint(request.getIntentHint(), request.getMessage());
        request.setIntentHint(normalizedIntentHint);

        List<String> resolvedRequestedDomains = resolveRequestedProfileDomains(normalizedIntentHint, request.getMessage(), request.getRequestedProfileDomains());
        request.setRequestedProfileDomains(resolvedRequestedDomains);

        logStructuredChatRequest(
                requestId,
                memberId,
                request,
                normalizedIntentHint,
                resolvedRequestedDomains,
                messageLength,
                isManualMode,
                hasManualContext
        );

        if (requiresSelectedChild(normalizedIntentHint, resolvedRequestedDomains) && request.getChildId() == null) {
            AiChatResponse clarifyResponse = buildMissingChildClarifyResponse(request, requestId, normalizedIntentHint);
            applyMetaExposurePolicy(clarifyResponse, memberId, requestId);
            logStructuredChatResponse(requestId, memberId, clarifyResponse, 0L);
            return clarifyResponse;
        }

        boolean isGrowthIntent = "GROWTH_CHECK".equals(normalizedIntentHint);

        ChildProfilePromptService.ResolvedProfileContext resolvedProfileContext = ChildProfilePromptService.ResolvedProfileContext.empty();
        if (isManualMode) {
            if (request.getChildId() != null) {
                resolvedProfileContext = childProfilePromptService.resolveProfileContext(memberId, request.getChildId());
                request.setChildId(resolvedProfileContext.getChildId());
            }
            request.setProfileContext(hasManualContext ? sanitizedProfileContext : "");
        } else {
            resolvedProfileContext = childProfilePromptService.resolveProfileContext(memberId, request.getChildId());
            if (resolvedProfileContext.getChildId() != null) {
                request.setChildId(resolvedProfileContext.getChildId());
            }
            request.setProfileContext(sanitizeProfileContext(resolvedProfileContext.getProfileContext()));
        }

        if (isGrowthIntent && resolvedProfileContext.getChildId() != null && (request.getGrowthContext() == null || request.getGrowthContext().isEmpty())) {
            request.setGrowthContext(resolvedProfileContext.getGrowthContext());
        }

        if (request.getGrowthContext() != null && !request.getGrowthContext().isEmpty()) {
            Object growthDataSource = request.getGrowthContext().get("data_source");
            log.info(
                    "Growth context prepared. childId={}, dataSource={}, keys={}",
                    request.getChildId(),
                    growthDataSource,
                    request.getGrowthContext().keySet()
            );
            log.debug("Growth context payload: {}", request.getGrowthContext());
        } else {
            log.warn("Growth context is empty. requestId={}, childId={}", requestId, request.getChildId());
        }

        Duration aiChatTimeout = resolveAiChatTimeout();
        log.info(
                "Forwarding AI chat request. requestId={}, childId={}, contextMode={}, intentHint={}, requestedProfileDomains={}, sessionId={}, messageLength={}, hasManualContext={}, hasResolvedProfileContext={}, hasGrowthContext={}, aiTimeoutSec={}",
                requestId,
                request.getChildId(),
                contextMode,
                request.getIntentHint(),
                request.getRequestedProfileDomains(),
                request.getSessionId(),
                messageLength,
                hasManualContext,
                resolvedProfileContext.hasProfileContext(),
                request.getGrowthContext() != null && !request.getGrowthContext().isEmpty(),
                aiChatTimeout.getSeconds()
        );

        boolean hasAiInternalToken = StringUtils.hasText(aiInternalToken);
        if (!hasAiInternalToken) {
            log.warn("AI internal token is not configured. requestId={}", requestId);
        }

        try {
            var requestBuilder = aiWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/chat").build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Request-Id", requestId);

            if (hasAiInternalToken) {
                requestBuilder = requestBuilder.header(INTERNAL_SERVICE_TOKEN_HEADER, aiInternalToken);
            }

            AiChatResponse response = requestBuilder
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiChatResponse.class)
                    .timeout(aiChatTimeout)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofMillis(500))
                                    .maxBackoff(Duration.ofSeconds(2))
                                    .jitter(0.5)
                                    .filter(this::isRetryable)
                                    .doBeforeRetry(signal -> log.warn(
                                            "Retrying AI service call. requestId={}, attempt={}, reason={}",
                                            requestId,
                                            signal.totalRetries() + 1,
                                            signal.failure().getClass().getSimpleName()
                                    ))
                    )
                    .onErrorMap(throwable -> mapToAiException(throwable, requestId))
                    .block();
            applyMetaExposurePolicy(response, memberId, requestId);
            logStructuredChatResponse(requestId, memberId, response, (System.nanoTime() - startedAt) / 1_000_000);
            return response;
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("AI chat request finished. requestId={}, elapsedMs={}", requestId, elapsedMs);
        }
    }

    private Duration resolveAiChatTimeout() {
        // Guardrail: keep timeout in an operable window even when env is misconfigured.
        int normalizedSeconds = Math.max(15, Math.min(180, aiChatTimeoutSeconds));
        return Duration.ofSeconds(normalizedSeconds);
    }

    public void recordChatFeedback(AiChatFeedbackRequest request, UUID memberId, String requestId) {
        String normalizedRating = request.getRating() == null ? null : request.getRating().trim().toUpperCase(Locale.ROOT);
        String normalizedReasonCode = request.getReasonCode() == null ? null : request.getReasonCode().trim().toUpperCase(Locale.ROOT);
        String normalizedResponseMode = request.getResponseMode() == null ? null : request.getResponseMode().trim().toUpperCase(Locale.ROOT);
        String normalizedIntent = request.getIntent() == null ? null : request.getIntent().trim().toUpperCase(Locale.ROOT);

        log.info(
                "AI_CHAT_FEEDBACK request_id={} session_id={} member_id_hash={} rating={} reason_code={} response_mode={} intent={} is_first_ai_answer={}",
                StringUtils.hasText(requestId) ? requestId : request.getRequestId(),
                request.getSessionId(),
                memberHash(memberId),
                normalizedRating,
                normalizedReasonCode,
                normalizedResponseMode,
                normalizedIntent,
                request.getFirstAiAnswer()
        );
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable cause = Exceptions.unwrap(throwable);
        if (cause instanceof WebClientRequestException) {
            return true;
        }

        if (cause instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }

        return false;
    }

    private void logStructuredChatRequest(
            String requestId,
            UUID memberId,
            AiChatRequest request,
            String intent,
            List<String> requestedDomains,
            int messageLength,
            boolean isManualMode,
            boolean hasManualContext
    ) {
        log.info(
                "AI_CHAT_REQUEST request_id={} member_id_hash={} session_id={} child_id_present={} intent={} requested_domains={} context_mode={} has_manual_context={} message_length={}",
                requestId,
                memberHash(memberId),
                request.getSessionId(),
                request.getChildId() != null,
                intent,
                requestedDomains,
                isManualMode ? "MANUAL" : "AUTO",
                hasManualContext,
                messageLength
        );
    }

    private void logStructuredChatResponse(String requestId, UUID memberId, AiChatResponse response, long elapsedMs) {
        AiChatMeta meta = response == null ? null : response.getMeta();
        int citationCount = meta != null && meta.getCitations() != null ? meta.getCitations().size() : 0;
        int quickActionCount = meta != null && meta.getQuickActions() != null ? meta.getQuickActions().size() : 0;
        String responseMode = meta != null && StringUtils.hasText(meta.getResponseMode()) ? meta.getResponseMode() : "ANSWER";
        String confidenceBucket = "none";
        if (meta != null && meta.getConfidence() != null) {
            double confidence = meta.getConfidence();
            if (confidence >= 0.85d) {
                confidenceBucket = "high";
            } else if (confidence >= 0.65d) {
                confidenceBucket = "medium";
            } else {
                confidenceBucket = "low";
            }
        }

        log.info(
                "AI_CHAT_RESPONSE request_id={} session_id={} member_id_hash={} response_mode={} intent={} elapsed_ms={} fallback_code={} citation_count={} quick_action_count={} confidence_bucket={} meta_exposed={}",
                requestId,
                response == null ? null : response.getSessionId(),
                memberHash(memberId),
                responseMode,
                meta == null ? null : meta.getIntent(),
                elapsedMs,
                meta == null ? null : meta.getFallbackCode(),
                citationCount,
                quickActionCount,
                confidenceBucket,
                meta != null
        );
    }

    private boolean requiresSelectedChild(String intentHint, List<String> requestedProfileDomains) {
        String normalizedIntent = normalizeIntentHint(intentHint);
        if ("GROWTH_CHECK".equals(normalizedIntent) || "MEDICAL".equals(normalizedIntent) || "ALLERGY".equals(normalizedIntent)) {
            return true;
        }
        return false;
    }

    private AiChatResponse buildMissingChildClarifyResponse(AiChatRequest request, String requestId, String intentHint) {
        String normalizedIntent = normalizeIntentHint(intentHint);
        String reply = "성장/의료 관련 답변을 위해 먼저 상단 헤더에서 자녀를 선택해 주세요.";
        if (!requiresSelectedChild(normalizedIntent, request.getRequestedProfileDomains())) {
            reply = "먼저 상단 헤더에서 자녀를 선택해 주세요.";
        } else if ("GROWTH_CHECK".equals(normalizedIntent)) {
            reply = "성장 확인을 위해 먼저 상단 헤더에서 자녀를 선택해 주세요.";
        }

        AiChatMeta meta = AiChatMeta.builder()
                .requestId(requestId)
                .responseMode("CLARIFY")
                .intent(StringUtils.hasText(normalizedIntent) ? normalizedIntent : "AUTO")
                .clarification(
                        AiChatClarification.builder()
                                .question("어느 아이 기준으로 확인할까요?")
                                .missingFields(List.of("child_selection"))
                                .options(List.of(
                                        buildNavigateAction("select_child", "자녀 선택하기", "/dashboard")
                                ))
                                .build()
                )
                .quickActions(List.of(
                        buildNavigateAction("open_record", "성장 기록 화면 열기", "/record")
                ))
                .citations(List.of(
                        AiChatCitation.builder()
                                .label("입력 조건 확인")
                                .sourceType("SYSTEM_POLICY")
                                .note("자녀 선택이 필요한 질문입니다.")
                                .build()
                ))
                .build();

        return AiChatResponse.builder()
                .reply(reply)
                .sessionId(request.getSessionId())
                .timestamp(OffsetDateTime.now().toString())
                .meta(meta)
                .build();
    }

    private AiChatQuickAction buildNavigateAction(String id, String label, String route) {
        return AiChatQuickAction.builder()
                .id(id)
                .label(label)
                .actionType("NAVIGATE")
                .route(route)
                .build();
    }

    private void applyMetaExposurePolicy(AiChatResponse response, UUID memberId, String requestId) {
        if (response == null || response.getMeta() == null) {
            return;
        }

        boolean exposeMeta = shouldExposeMetaToMember(memberId);
        if (!exposeMeta) {
            log.debug("AI chat meta hidden by rollout policy. requestId={}, memberIdHash={}", requestId, memberHash(memberId));
            response.setMeta(null);
        } else if (!StringUtils.hasText(response.getMeta().getRequestId())) {
            response.getMeta().setRequestId(requestId);
        }
    }

    private boolean shouldExposeMetaToMember(UUID memberId) {
        if (!aiChatMetaEnabled) {
            return false;
        }
        if (memberId == null) {
            return false;
        }
        int rolloutPercent = Math.max(0, Math.min(100, aiChatMetaRolloutPercent));
        if (rolloutPercent >= 100) {
            return true;
        }
        if (rolloutPercent <= 0) {
            return false;
        }
        int bucket = Math.floorMod(memberId.toString().hashCode(), 100);
        return bucket < rolloutPercent;
    }

    private String memberHash(UUID memberId) {
        if (memberId == null) {
            return "none";
        }
        return Integer.toHexString(memberId.toString().hashCode());
    }

    private String normalizeIntentHint(String intentHint) {
        if (!StringUtils.hasText(intentHint)) {
            return null;
        }
        return intentHint.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveIntentHint(String intentHint, String message) {
        String normalizedIntentHint = normalizeIntentHint(intentHint);
        if (StringUtils.hasText(normalizedIntentHint)) {
            if ("GROWTH".equals(normalizedIntentHint) || "GROWTH_CHECK".equals(normalizedIntentHint) || "GROWTH_QUERY".equals(normalizedIntentHint)) {
                return "GROWTH_CHECK";
            }

            if ("ALLERGY_QUERY".equals(normalizedIntentHint)) {
                return "ALLERGY";
            }

            if (SUPPORTED_INTENTS.contains(normalizedIntentHint)) {
                return normalizedIntentHint;
            }
        }

        if (isGrowthCheckIntent(message)) {
            return "GROWTH_CHECK";
        }

        return detectIntentFromMessage(message);
    }

    private List<String> normalizeRequestedProfileDomains(List<String> requestedProfileDomains) {
        if (requestedProfileDomains == null || requestedProfileDomains.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String rawDomain : requestedProfileDomains) {
            if (!StringUtils.hasText(rawDomain)) {
                continue;
            }

            String normalizedDomain = rawDomain.trim().toLowerCase(Locale.ROOT);
            if (PROFILE_DOMAIN_SET.contains(normalizedDomain)) {
                normalized.add(normalizedDomain);
            }
        }

        if (normalized.isEmpty()) {
            return List.of();
        }

        return normalized.stream().distinct().toList();
    }

    private String detectIntentFromMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "AUTO";
        }

        String normalized = message.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);

        if (normalized.contains("수면") || normalized.contains("낮잠") || normalized.contains("취침") || normalized.contains("기상")) {
            return "SLEEP";
        }

        if (normalized.contains("이유식") || normalized.contains("수유") || normalized.contains("식습관") || normalized.contains("식단") || normalized.contains("분유") || normalized.contains("먹")) {
            return "FEEDING";
        }

        if (normalized.contains("발달") || normalized.contains("마일스톤") || normalized.contains("성향")) {
            return "DEVELOPMENT";
        }

        if (normalized.contains("예방접종") || normalized.contains("백신") || normalized.contains("접종")) {
            return "VACCINATION";
        }

        if (normalized.contains("루틴") || normalized.contains("일정") || normalized.contains("생활패턴")) {
            return "ROUTINE";
        }

        if (normalized.contains("열") || normalized.contains("증상") || normalized.contains("병원") || normalized.contains("약") || normalized.contains("알레르기") || normalized.contains("응급")) {
            return "MEDICAL";
        }

        return "AUTO";
    }

    private List<String> resolveRequestedProfileDomains(String intentHint, String message, List<String> requestedProfileDomains) {
        String resolvedIntent = resolveIntentHint(intentHint, message);

        if (requestedProfileDomains != null && !requestedProfileDomains.isEmpty()) {
            List<String> sanitized = normalizeRequestedProfileDomains(requestedProfileDomains);
            if (!sanitized.isEmpty()) {
                return sanitized;
            }
        }

        if (resolvedIntent == null) {
            return FALLBACK_PROFILE_DOMAINS;
        }

        return switch (resolvedIntent) {
            case "GROWTH_CHECK" -> List.of("growth");
            case "SLEEP" -> List.of("sleep", "routine");
            case "FEEDING" -> List.of("feeding", "routine");
            case "DEVELOPMENT" -> List.of("development");
            case "VACCINATION" -> List.of("vaccination", "medical");
            case "ROUTINE" -> List.of("routine", "sleep");
            case "MEDICAL", "ALLERGY" -> List.of("medical", "allergy", "safety");
            case "AUTO" -> FALLBACK_PROFILE_DOMAINS;
            default -> FALLBACK_PROFILE_DOMAINS;
        };
    }

    private boolean isGrowthCheckIntent(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }

        String normalized = message.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (normalized.contains("백분위") || normalized.contains("성장곡선")) {
            return true;
        }

        if (normalized.contains("성장")
                && (normalized.contains("확인")
                || normalized.contains("분석")
                || normalized.contains("또래")
                || normalized.contains("정상"))) {
            return true;
        }

        boolean hasMeasureKeyword = normalized.contains("키")
                || normalized.contains("몸무게")
                || normalized.contains("체중")
                || normalized.contains("신장");
        boolean hasComparisonKeyword = normalized.contains("또래")
                || normalized.contains("정상")
                || normalized.contains("평균")
                || normalized.contains("백분위");

        if (hasMeasureKeyword && hasComparisonKeyword) {
            return true;
        }

        return normalized.contains("성장발달") && normalized.contains("확인");
    }

    private Throwable mapToAiException(Throwable throwable, String requestId) {
        Throwable cause = Exceptions.unwrap(throwable);
        if (cause instanceof AiException) {
            return cause;
        }

        if (cause instanceof TimeoutException) {
            return new AiException(AiException.AiErrorCode.TIMEOUT);
        }

        if (cause instanceof WebClientResponseException responseException) {
            log.warn(
                    "AI service returned error. requestId={}, status={}, responseBody={}",
                    requestId,
                    responseException.getStatusCode().value(),
                    responseException.getResponseBodyAsString()
            );
            if (responseException.getStatusCode().is4xxClientError()) {
                return new AiException(AiException.AiErrorCode.BAD_REQUEST);
            }
            return new AiException(AiException.AiErrorCode.UPSTREAM_ERROR);
        }

        if (cause instanceof WebClientRequestException) {
            return new AiException(AiException.AiErrorCode.UNAVAILABLE);
        }

        return new AiException(AiException.AiErrorCode.UPSTREAM_ERROR);
    }

    private static final int PROFILE_CONTEXT_MAX_LENGTH = 4000;

    private String sanitizeProfileContext(String profileContext) {
        if (!StringUtils.hasText(profileContext)) {
            return profileContext;
        }

        String normalized = PROFILE_CONTEXT_CONTROL_PATTERN.matcher(profileContext).replaceAll(" ");
        normalized = normalized.replaceAll("[\t\r\n]", " ");
        normalized = PROFILE_CONTEXT_MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ");
        String sanitized = normalized.strip();
        if (sanitized.length() > PROFILE_CONTEXT_MAX_LENGTH) {
            return sanitized.substring(0, PROFILE_CONTEXT_MAX_LENGTH);
        }
        return sanitized;
    }
}
