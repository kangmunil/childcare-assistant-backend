package com.childcare.domain.ai.service;

import com.childcare.domain.ai.dto.AiChatRequest;
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

        log.info(
                "Forwarding AI chat request. requestId={}, childId={}, contextMode={}, intentHint={}, requestedProfileDomains={}, sessionId={}, messageLength={}, hasManualContext={}, hasResolvedProfileContext={}, hasGrowthContext={}",
                requestId,
                request.getChildId(),
                contextMode,
                request.getIntentHint(),
                request.getRequestedProfileDomains(),
                request.getSessionId(),
                messageLength,
                hasManualContext,
                resolvedProfileContext.hasProfileContext(),
                request.getGrowthContext() != null && !request.getGrowthContext().isEmpty()
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

            return requestBuilder
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiChatResponse.class)
                    .timeout(Duration.ofSeconds(35))
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
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("AI chat request finished. requestId={}, elapsedMs={}", requestId, elapsedMs);
        }
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
