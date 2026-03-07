package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.service.BoardFileService;
import com.childcare.global.config.JwtAuthenticationFilter;
import com.childcare.global.dto.ApiResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardFileController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardFileService boardFileService;

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
    void uploadFilesAcceptsMultipartFilesField() throws Exception {
        BoardFileDto dto = BoardFileDto.builder()
                .id(101L)
                .itemId(9L)
                .orgFilename("contract.png")
                .fileName("stored.png")
                .filePath("board/free/20260228/stored.png")
                .extension("png")
                .fileSize(123)
                .downloadUrl("https://example.com/signed.png")
                .build();

        when(boardFileService.uploadFiles(any(UUID.class), eq(5L), eq(9L), any()))
                .thenReturn(ApiResponse.success("파일 업로드 성공", List.of(dto)));

        MockMultipartFile multipartFile = new MockMultipartFile(
                "files",
                "contract.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/boards/{boardId}/items/{itemId}/files", 5L, 9L)
                        .file(multipartFile)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].orgFilename").value("contract.png"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartFile>> filesCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(boardFileService).uploadFiles(eq(memberId), eq(5L), eq(9L), filesCaptor.capture());
        assertEquals(1, filesCaptor.getValue().size());
        assertEquals("contract.png", filesCaptor.getValue().get(0).getOriginalFilename());
    }

    @Test
    void downloadFileReturnsDownloadUrlMap() throws Exception {
        when(boardFileService.getDownloadUrl(eq(memberId), eq(5L), eq(9L), eq(11L)))
                .thenReturn("https://example.com/files/11");

        mockMvc.perform(get("/boards/{boardId}/items/{itemId}/files/{fileId}/download", 5L, 9L, 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://example.com/files/11"));
    }

    @Test
    void getFilesWithoutAuthenticationReturns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/boards/{boardId}/items/{itemId}/files", 5L, 9L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verify(boardFileService, never()).getFiles(any(UUID.class), anyLong(), anyLong());
    }
}
