package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardCommentDto;
import com.childcare.domain.board.dto.BoardCommentRequest;
import com.childcare.domain.board.service.BoardCommentService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/boards/{boardId}/items/{itemId}/comments")
@RequiredArgsConstructor
@Slf4j
public class BoardCommentController {

    private final BoardCommentService boardCommentService;

    /**
     * 댓글 목록 조회
     * GET /boards/{boardId}/items/{itemId}/comments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardCommentDto>>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Get comments for item: {}, member: {}", itemId, memberId);

        ApiResponse<List<BoardCommentDto>> response = boardCommentService.getComments(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 작성
     * POST /boards/{boardId}/items/{itemId}/comments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BoardCommentDto>> createComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @RequestBody BoardCommentRequest request) {
        UUID memberId = getMemberId();
        log.info("Create comment for item: {}, member: {}", itemId, memberId);

        ApiResponse<BoardCommentDto> response = boardCommentService.createComment(memberId, boardId, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 수정
     * PUT /boards/{boardId}/items/{itemId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<BoardCommentDto>> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId,
            @RequestBody BoardCommentRequest request) {
        UUID memberId = getMemberId();
        log.info("Update comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        ApiResponse<BoardCommentDto> response = boardCommentService.updateComment(memberId, boardId, itemId, commentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제
     * DELETE /boards/{boardId}/items/{itemId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        log.info("Delete comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        ApiResponse<Void> response = boardCommentService.deleteComment(memberId, boardId, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 공감
     * POST /boards/{boardId}/items/{itemId}/comments/{commentId}/like
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Integer>> likeComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        log.info("Like comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        ApiResponse<Integer> response = boardCommentService.likeComment(memberId, boardId, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 공감 취소
     * DELETE /boards/{boardId}/items/{itemId}/comments/{commentId}/like
     */
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        log.info("Unlike comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        ApiResponse<Integer> response = boardCommentService.unlikeComment(memberId, boardId, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
