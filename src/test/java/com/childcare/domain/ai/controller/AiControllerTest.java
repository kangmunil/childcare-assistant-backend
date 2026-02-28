package com.childcare.domain.ai.controller;

import com.childcare.domain.ai.dto.AiChatRequest;
import com.childcare.domain.ai.dto.AiChatResponse;
import com.childcare.domain.ai.dto.AiChatFeedbackRequest;
import com.childcare.domain.ai.service.AiService;
import com.childcare.global.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID memberId;

    @BeforeEach
    void setUpSecurityContext() {
        memberId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatAcceptsSnakeCasePayloadAndReturnsWrappedResponse() throws Exception {
        AiChatResponse response = AiChatResponse.builder()
                .reply("ok")
                .sessionId("session-1")
                .timestamp("2026-02-28T19:00:00")
                .build();
        doNothing().when(aiService).recordChatFeedback(any(AiChatFeedbackRequest.class), any(UUID.class), anyString());
        org.mockito.Mockito.when(aiService.chat(any(AiChatRequest.class), anyString(), any(UUID.class)))
                .thenReturn(response);

        String payload = """
                {
                  "message": "수면 루틴 알려줘",
                  "session_id": "session-client-1",
                  "child_id": 7,
                  "context_mode": "AUTO",
                  "intent_hint": "SLEEP",
                  "requested_profile_domains": ["sleep", "routine"]
                }
                """;

        mockMvc.perform(post("/ai/chat")
                        .header("X-Request-Id", "req-chat-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-chat-1"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.reply").value("ok"))
                .andExpect(jsonPath("$.data.session_id").value("session-1"));

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiService).chat(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq("req-chat-1"), org.mockito.ArgumentMatchers.eq(memberId));
        AiChatRequest captured = requestCaptor.getValue();
        assertEquals("수면 루틴 알려줘", captured.getMessage());
        assertEquals("session-client-1", captured.getSessionId());
        assertEquals(7L, captured.getChildId());
        assertEquals("AUTO", captured.getEffectiveContextMode());
        assertEquals("SLEEP", captured.getIntentHint());
        assertEquals(List.of("sleep", "routine"), captured.getRequestedProfileDomains());
        assertEquals(memberId.toString(), captured.getUserId());
    }

    @Test
    void submitChatFeedbackReturnsSuccess() throws Exception {
        AiChatFeedbackRequest request = AiChatFeedbackRequest.builder()
                .sessionId("s-1")
                .requestId("req-1")
                .rating("UP")
                .responseMode("ANSWER")
                .intent("VACCINATION")
                .build();

        doNothing().when(aiService).recordChatFeedback(any(AiChatFeedbackRequest.class), any(UUID.class), anyString());

        mockMvc.perform(post("/ai/chat/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("피드백이 접수되었습니다."));

        ArgumentCaptor<AiChatFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(AiChatFeedbackRequest.class);
        verify(aiService).recordChatFeedback(requestCaptor.capture(), any(UUID.class), anyString());
        assertEquals("UP", requestCaptor.getValue().getRating());
    }

    @Test
    void submitChatFeedbackWithInvalidRatingReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "session_id": "s-2",
                  "request_id": "req-2",
                  "rating": "MAYBE",
                  "reason_code": "INVALID_REASON",
                  "response_mode": "ANSWER"
                }
                """;

        mockMvc.perform(post("/ai/chat/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AI_003_BAD_REQUEST"));

        verify(aiService, never()).recordChatFeedback(any(AiChatFeedbackRequest.class), any(UUID.class), anyString());
    }
}
