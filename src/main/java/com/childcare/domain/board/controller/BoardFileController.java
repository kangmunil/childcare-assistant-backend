package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.entity.BoardFile;
import com.childcare.domain.board.service.BoardFileService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        Long memberSeq = getMemberSeq();
        log.info("Get files for item: {}, member: {}", itemId, memberSeq);

        ApiResponse<List<BoardFileDto>> response = boardFileService.getFiles(memberSeq, boardId, itemId);
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
        Long memberSeq = getMemberSeq();
        log.info("Upload files for item: {}, member: {}, count: {}", itemId, memberSeq, files.size());

        ApiResponse<List<BoardFileDto>> response = boardFileService.uploadFiles(memberSeq, boardId, itemId, files);
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 다운로드
     * GET /boards/{boardId}/items/{itemId}/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long fileId) {
        Long memberSeq = getMemberSeq();
        log.info("Download file: {} for item: {}, member: {}", fileId, itemId, memberSeq);

        Resource resource = boardFileService.downloadFile(memberSeq, boardId, itemId, fileId);
        BoardFile fileInfo = boardFileService.getFileInfo(fileId);

        String encodedFilename = URLEncoder.encode(fileInfo.getOrgFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
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
        Long memberSeq = getMemberSeq();
        log.info("Delete file: {} for item: {}, member: {}", fileId, itemId, memberSeq);

        ApiResponse<Void> response = boardFileService.deleteFile(memberSeq, boardId, itemId, fileId);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        return SecurityUtil.getCurrentMemberSeq();
    }
}
