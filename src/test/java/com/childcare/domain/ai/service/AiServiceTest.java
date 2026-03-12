package com.childcare.domain.ai.service;

import com.childcare.domain.ai.dto.AiChatRequest;
import com.childcare.domain.ai.dto.AiChatMeta;
import com.childcare.domain.ai.dto.AiChatResponse;
import com.childcare.domain.profile.service.ChildProfilePromptService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatIncludesResolvedProfileContextInAiPayload() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s1")
                        .timestamp("2026-02-15T12:00:00")
                        .meta(AiChatMeta.builder().responseMode("ANSWER").intent("GROWTH_CHECK").build())
                        .build())
        );

        UUID memberId = UUID.randomUUID();
        when(promptService.resolveProfileContext(memberId, 10L)).thenReturn(
                new ChildProfilePromptService.ResolvedProfileContext(
                        10L,
                        "[자녀 프로필 - 신뢰 데이터]\n- 알레르기: 계란",
                        Map.of(
                                "gender", "M",
                                "birth_date", "2024-01-01",
                                "height_cm", 82.4d,
                                "weight_kg", 11.2d,
                                "measured_date", "2026-02-10",
                                "stale_days", 5,
                                "data_source", "history"
                        )
                )
        );

        AiService aiService = new AiService(
                webClient,
                "http://localhost:8000",
                "secure-token",
                promptService
        );
        setPrivateField(aiService, "aiChatMetaEnabled", true);
        setPrivateField(aiService, "aiChatMetaRolloutPercent", 100);

        AiChatRequest request = AiChatRequest.builder()
                .message("아이 성장발달 확인하고 싶어")
                .sessionId("s1")
                .childId(10L)
                .userId(memberId.toString())
                .build();

        AiChatResponse response = aiService.chat(request, "req-1", memberId);

        ArgumentCaptor<AiChatRequest> payloadCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(requestBodySpec).bodyValue(payloadCaptor.capture());

        AiChatRequest payload = payloadCaptor.getValue();
        assertEquals(10L, payload.getChildId());
        assertEquals("[자녀 프로필 - 신뢰 데이터] - 알레르기: 계란", payload.getProfileContext());
        assertEquals("AUTO", payload.getContextMode());
        assertEquals("GROWTH_CHECK", payload.getIntentHint());
        assertEquals("history", payload.getGrowthContext().get("data_source"));
        assertEquals(List.of("growth"), payload.getRequestedProfileDomains());
        assertEquals("ok", response.getReply());
        assertNotNull(response.getMeta());
        assertEquals("ANSWER", response.getMeta().getResponseMode());
        verify(requestBodySpec, times(2)).header(anyString(), anyString());
        verify(requestBodySpec).header("X-Internal-Service-Token", "secure-token");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatUsesRequestedDomainsFromClientWhenProvided() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);
        when(promptService.resolveProfileContext(any(), any())).thenReturn(
                ChildProfilePromptService.ResolvedProfileContext.empty()
        );

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s5")
                        .timestamp("2026-02-15T12:40:00")
                        .build())
        );

        AiService aiService = new AiService(
                webClient,
                "http://localhost:8000",
                "",
                promptService
        );

        AiChatRequest request = AiChatRequest.builder()
                .message("지금 루틴이 너무 불규칙해요")
                .sessionId("s5")
                .childId(10L)
                .requestedProfileDomains(List.of("sleep", "routine", "invalid"))
                .userId("user")
                .build();

        AiChatResponse response = aiService.chat(request, "req-5", UUID.randomUUID());

        ArgumentCaptor<AiChatRequest> payloadCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(requestBodySpec).bodyValue(payloadCaptor.capture());

        AiChatRequest payload = payloadCaptor.getValue();
        assertEquals("sleep", payload.getRequestedProfileDomains().get(0));
        assertEquals("routine", payload.getRequestedProfileDomains().get(1));
        assertEquals(2, payload.getRequestedProfileDomains().size());
        assertEquals("ROUTINE", payload.getIntentHint());
        assertEquals("ok", response.getReply());
        assertEquals(null, payload.getGrowthContext());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatUsesManualProfileContextWhenContextModeIsManual() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s2")
                        .timestamp("2026-02-15T12:10:00")
                        .build())
        );

        UUID memberId = UUID.randomUUID();
        AiService aiService = new AiService(
                webClient,
                "http://localhost:8000",
                "",
                promptService
        );

        String manualContext = "[사용자 입력]\n- 계란 알레르기";
        AiChatRequest request = AiChatRequest.builder()
                .message("수동 정보로 답변해줘")
                .sessionId("s2")
                .contextMode("MANUAL")
                .profileContext(manualContext)
                .userId(memberId.toString())
                .build();

        AiChatResponse response = aiService.chat(request, "req-2", memberId);

        ArgumentCaptor<AiChatRequest> payloadCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(requestBodySpec).bodyValue(payloadCaptor.capture());

        AiChatRequest payload = payloadCaptor.getValue();
        verify(promptService, never()).resolveProfileContext(any(), any());
        assertEquals("MANUAL", payload.getContextMode());
        assertEquals("[사용자 입력] - 계란 알레르기", payload.getProfileContext());
        assertEquals("ok", response.getReply());
        verify(requestBodySpec, times(1)).header("X-Request-Id", "req-2");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatWithoutInternalTokenDoesNotInjectInternalHeader() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(promptService.resolveProfileContext(any(), any())).thenReturn(
                ChildProfilePromptService.ResolvedProfileContext.empty()
        );

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s3")
                        .timestamp("2026-02-15T12:20:00")
                        .build())
        );

        AiService aiService = new AiService(webClient, "http://localhost:8000", "", promptService);

        AiChatRequest request = AiChatRequest.builder()
                .message("안녕")
                .sessionId("s3")
                .userId("user")
                .build();

        aiService.chat(request, "req-3", UUID.randomUUID());

        verify(requestBodySpec).header("X-Request-Id", "req-3");
        verify(requestBodySpec, never()).header("X-Internal-Service-Token", "");
        verify(requestBodySpec, never()).header(eq("X-Internal-Service-Token"), anyString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatSanitizesAndTruncatesManualProfileContext() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s4")
                        .timestamp("2026-02-15T12:30:00")
                        .build())
        );

        AiService aiService = new AiService(
                webClient,
                "http://localhost:8000",
                "",
                promptService
        );

        String longManualContext = ("  위험: " + "a".repeat(5000));
        AiChatRequest request = AiChatRequest.builder()
                .message("수동 입력 긴 텍스트")
                .sessionId("s4")
                .contextMode("MANUAL")
                .profileContext(longManualContext)
                .userId("user")
                .build();

        aiService.chat(request, "req-4", UUID.randomUUID());

        ArgumentCaptor<AiChatRequest> payloadCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(requestBodySpec).bodyValue(payloadCaptor.capture());
        assertEquals(4000, payloadCaptor.getValue().getProfileContext().length());
        assertEquals("위험:", payloadCaptor.getValue().getProfileContext().substring(0, 3));
    }

    @Test
    void chatReturnsClarifyResponseWhenChildSelectionIsRequired() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        AiService aiService = new AiService(webClient, "http://localhost:8000", "", promptService);
        setPrivateField(aiService, "aiChatMetaEnabled", true);
        setPrivateField(aiService, "aiChatMetaRolloutPercent", 100);

        AiChatRequest request = AiChatRequest.builder()
                .message("아이 성장발달 확인해줘")
                .sessionId("s-clarify")
                .userId("user")
                .build();

        AiChatResponse response = aiService.chat(request, "req-clarify", UUID.randomUUID());

        assertEquals("s-clarify", response.getSessionId());
        assertNotNull(response.getMeta());
        assertEquals("CLARIFY", response.getMeta().getResponseMode());
        assertNotNull(response.getMeta().getClarification());
        verify(webClient, never()).post();
        verify(promptService, never()).resolveProfileContext(any(), any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void chatHidesMetaWhenRolloutDisabled() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        ChildProfilePromptService promptService = mock(ChildProfilePromptService.class);

        when(promptService.resolveProfileContext(any(), any())).thenReturn(ChildProfilePromptService.ResolvedProfileContext.empty());
        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(
                Mono.just(AiChatResponse.builder()
                        .reply("ok")
                        .sessionId("s-meta")
                        .timestamp("2026-02-26T12:00:00")
                        .meta(AiChatMeta.builder().responseMode("ANSWER").intent("AUTO").build())
                        .build())
        );

        AiService aiService = new AiService(webClient, "http://localhost:8000", "", promptService);
        setPrivateField(aiService, "aiChatMetaEnabled", false);
        setPrivateField(aiService, "aiChatMetaRolloutPercent", 100);

        AiChatRequest request = AiChatRequest.builder()
                .message("안녕")
                .sessionId("s-meta")
                .userId("user")
                .build();

        AiChatResponse response = aiService.chat(request, "req-meta", UUID.randomUUID());

        assertNull(response.getMeta());
    }
}
