package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardCommentDto;
import com.childcare.domain.board.dto.BoardCommentRequest;
import com.childcare.domain.board.service.BoardCommentService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BoardCommentController {

    private final BoardCommentService boardCommentService;
    private static final java.util.Set<String> RESERVED_SLUGS = java.util.Set.of(
            "new", "edit", "admin", "delete", "api", "manage", "search"
    );

    /**
     * 댓글 목록 조회
     * GET /boards/{boardId}/items/{itemId}/comments
     */
    @GetMapping("/{boardId:\\d+}/items/{itemId}/comments")
    public ResponseEntity<ApiResponse<List<BoardCommentDto>>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Get comments for item: {}, member: {}", itemId, memberId);

        ApiResponse<List<BoardCommentDto>> response = boardCommentService.getComments(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 목록 조회 (slug 기반)
     * GET /boards/{slug}/items/{itemId}/comments
     */
    @GetMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments")
    public ResponseEntity<ApiResponse<List<BoardCommentDto>>> getCommentsBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<List<BoardCommentDto>> response = boardCommentService.getCommentsBySlug(memberId, normalizedSlug, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 작성
     * POST /boards/{boardId}/items/{itemId}/comments
     */
    @PostMapping("/{boardId:\\d+}/items/{itemId}/comments")
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
     * 댓글 작성 (slug 기반)
     * POST /boards/{slug}/items/{itemId}/comments
     */
    @PostMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments")
    public ResponseEntity<ApiResponse<BoardCommentDto>> createCommentBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId,
            @RequestBody BoardCommentRequest request) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<BoardCommentDto> response = boardCommentService.createCommentBySlug(memberId, normalizedSlug, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 수정
     * PUT /boards/{boardId}/items/{itemId}/comments/{commentId}
     */
    @PutMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}")
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
     * 댓글 수정 (slug 기반)
     * PUT /boards/{slug}/items/{itemId}/comments/{commentId}
     */
    @PutMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<BoardCommentDto>> updateCommentBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId,
            @RequestBody BoardCommentRequest request) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<BoardCommentDto> response = boardCommentService.updateCommentBySlug(memberId, normalizedSlug, itemId, commentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제
     * DELETE /boards/{boardId}/items/{itemId}/comments/{commentId}
     */
    @DeleteMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}")
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
     * 댓글 삭제 (slug 기반)
     * DELETE /boards/{slug}/items/{itemId}/comments/{commentId}
     */
    @DeleteMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteCommentBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<Void> response = boardCommentService.deleteCommentBySlug(memberId, normalizedSlug, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 공감
     * POST /boards/{boardId}/items/{itemId}/comments/{commentId}/like
     */
    @PostMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}/like")
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
     * 댓글 공감 (slug 기반)
     * POST /boards/{slug}/items/{itemId}/comments/{commentId}/like
     */
    @PostMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Integer>> likeCommentBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<Integer> response = boardCommentService.likeCommentBySlug(memberId, normalizedSlug, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 공감 취소
     * DELETE /boards/{boardId}/items/{itemId}/comments/{commentId}/like
     */
    @DeleteMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        log.info("Unlike comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        ApiResponse<Integer> response = boardCommentService.unlikeComment(memberId, boardId, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 공감 취소 (slug 기반)
     * DELETE /boards/{slug}/items/{itemId}/comments/{commentId}/like
     */
    @DeleteMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeCommentBySlug(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9-]+$")
            @Size(min = 2, max = 50)
            String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<Integer> response = boardCommentService.unlikeCommentBySlug(memberId, normalizedSlug, itemId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 고정
     * POST /boards/{boardId}/items/{itemId}/comments/{commentId}/pin
     */
    @PostMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        return ResponseEntity.ok(boardCommentService.pinComment(memberId, boardId, itemId, commentId));
    }

    /**
     * 댓글 고정 (slug 기반)
     */
    @PostMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinCommentBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);
        return ResponseEntity.ok(boardCommentService.pinCommentBySlug(memberId, normalizedSlug, itemId, commentId));
    }

    /**
     * 댓글 고정 해제
     * DELETE /boards/{boardId}/items/{itemId}/comments/{commentId}/pin
     */
    @DeleteMapping("/{boardId:\\d+}/items/{itemId}/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<Void>> unpinComment(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        return ResponseEntity.ok(boardCommentService.unpinComment(memberId, boardId, itemId, commentId));
    }

    /**
     * 댓글 고정 해제 (slug 기반)
     */
    @DeleteMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<Void>> unpinCommentBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId,
            @PathVariable Long commentId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);
        return ResponseEntity.ok(boardCommentService.unpinCommentBySlug(memberId, normalizedSlug, itemId, commentId));
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }

    private String normalizeSlug(String slug) {
        String normalized = slug.toLowerCase();
        if (RESERVED_SLUGS.contains(normalized)) {
            throw new com.childcare.global.exception.BoardException(
                    com.childcare.global.exception.BoardException.BoardErrorCode.BOARD_SLUG_RESERVED
            );
        }
        return normalized;
    }
}
