package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.service.BoardFileService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/boards/{boardId}/items/{itemId}/files")
@RequiredArgsConstructor
@Slf4j
public class BoardFileController {

    private final BoardFileService boardFileService;

    /**
     * 파일 목록 조회
     * GET /boards/{boardId}/items/{itemId}/files
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardFileDto>>> getFiles(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Get files for item: {}, member: {}", itemId, memberId);

        ApiResponse<List<BoardFileDto>> response = boardFileService.getFiles(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 업로드
     * POST /boards/{boardId}/items/{itemId}/files
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<BoardFileDto>>> uploadFiles(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @RequestParam("files") List<MultipartFile> files) {
        UUID memberId = getMemberId();
        log.info("Upload files for item: {}, member: {}, count: {}", itemId, memberId, files.size());

        ApiResponse<List<BoardFileDto>> response = boardFileService.uploadFiles(memberId, boardId, itemId, files);
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 다운로드 (Supabase Storage로 리다이렉트)
     * GET /boards/{boardId}/items/{itemId}/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Void> downloadFile(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long fileId) {
        UUID memberId = getMemberId();
        log.info("Download file: {} for item: {}, member: {}", fileId, itemId, memberId);

        String downloadUrl = boardFileService.getDownloadUrl(memberId, boardId, itemId, fileId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }

    /**
     * 파일 삭제
     * DELETE /boards/{boardId}/items/{itemId}/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long fileId) {
        UUID memberId = getMemberId();
        log.info("Delete file: {} for item: {}, member: {}", fileId, itemId, memberId);

        ApiResponse<Void> response = boardFileService.deleteFile(memberId, boardId, itemId, fileId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
