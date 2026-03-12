package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardCommentDto;
import com.childcare.domain.board.dto.BoardCommentRequest;
import com.childcare.domain.board.service.BoardCommentService;
import com.childcare.global.config.JwtAuthenticationFilter;
import com.childcare.global.dto.ApiResponse;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardCommentService boardCommentService;

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
    void createCommentBySlugForwardsFrontendPayload() throws Exception {
        BoardCommentDto dto = BoardCommentDto.builder()
                .id(11L)
                .itemId(55L)
                .parentSeq(10L)
                .content("댓글 본문")
                .secretYn("Y")
                .isAuthor(true)
                .build();

        when(boardCommentService.createCommentBySlug(any(UUID.class), eq("community"), eq(55L), any(BoardCommentRequest.class)))
                .thenReturn(ApiResponse.success("댓글 작성 성공", dto));

        String payload = """
                {
                  "content": "댓글 본문",
                  "parentSeq": 10,
                  "secretYn": "Y"
                }
                """;

        mockMvc.perform(post("/boards/community/items/55/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.parentSeq").value(10))
                .andExpect(jsonPath("$.data.secretYn").value("Y"))
                .andExpect(jsonPath("$.data.isAuthor").value(true));

        ArgumentCaptor<BoardCommentRequest> captor = ArgumentCaptor.forClass(BoardCommentRequest.class);
        verify(boardCommentService).createCommentBySlug(eq(memberId), eq("community"), eq(55L), captor.capture());
        BoardCommentRequest request = captor.getValue();
        assertEquals("댓글 본문", request.getContent());
        assertEquals(10L, request.getParentSeq());
        assertEquals("Y", request.getSecretYn());
    }

    @Test
    void getCommentsBySlugReservedSlugReturnsBoard029() throws Exception {
        mockMvc.perform(get("/boards/admin/items/10/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BOARD_029"));

        verify(boardCommentService, never()).getCommentsBySlug(any(UUID.class), anyString(), anyLong());
    }

    @Test
    void getCommentsBySlugWithoutAuthenticationReturns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/boards/community/items/10/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verify(boardCommentService, never()).getCommentsBySlug(any(UUID.class), anyString(), anyLong());
    }
}
