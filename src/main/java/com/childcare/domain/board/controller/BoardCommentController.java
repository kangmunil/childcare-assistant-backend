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
        Long memberSeq = getMemberSeq();
        log.info("Get comments for item: {}, member: {}", itemId, memberSeq);

        ApiResponse<List<BoardCommentDto>> response = boardCommentService.getComments(memberSeq, boardId, itemId);
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
        Long memberSeq = getMemberSeq();
        log.info("Create comment for item: {}, member: {}", itemId, memberSeq);

        ApiResponse<BoardCommentDto> response = boardCommentService.createComment(memberSeq, boardId, itemId, request);
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
        Long memberSeq = getMemberSeq();
        log.info("Update comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        ApiResponse<BoardCommentDto> response = boardCommentService.updateComment(memberSeq, boardId, itemId, commentId, request);
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
        Long memberSeq = getMemberSeq();
        log.info("Delete comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        ApiResponse<Void> response = boardCommentService.deleteComment(memberSeq, boardId, itemId, commentId);
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
        Long memberSeq = getMemberSeq();
        log.info("Like comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        ApiResponse<Integer> response = boardCommentService.likeComment(memberSeq, boardId, itemId, commentId);
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
        Long memberSeq = getMemberSeq();
        log.info("Unlike comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        ApiResponse<Integer> response = boardCommentService.unlikeComment(memberSeq, boardId, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        return SecurityUtil.getCurrentMemberSeq();
    }
}
