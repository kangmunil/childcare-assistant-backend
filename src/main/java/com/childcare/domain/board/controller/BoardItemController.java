package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardItemDto;
import com.childcare.domain.board.dto.BoardItemRequest;
import com.childcare.domain.board.dto.BoardSearchRequest;
import com.childcare.domain.board.service.BoardItemService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BoardItemController {

    private final BoardItemService boardItemService;
    private static final java.util.Set<String> RESERVED_SLUGS = java.util.Set.of(
            "new", "edit", "admin", "delete", "api", "manage", "search");

    /**
     * 게시글 목록 조회
     * GET /boards/{boardId}/items?searchType=title&keyword=검색어&page=0&size=20
     */
    @GetMapping("/{boardId:\\d+}/items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItemList(
            @PathVariable Long boardId,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean includeHighlights) {
        UUID memberId = getMemberId();
        log.info("Get item list for board: {}, member: {}", boardId, memberId);

        BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                .searchType(searchType)
                .keyword(keyword)
                .category(category)
                .page(page)
                .size(size)
                .includeHighlights(includeHighlights)
                .build();

        ApiResponse<Map<String, Object>> response = boardItemService.getItemList(memberId, boardId, searchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 목록 조회 (slug 기반)
     * GET /boards/{slug}/items
     */
    @GetMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItemListBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean includeHighlights) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                .searchType(searchType)
                .keyword(keyword)
                .category(category)
                .page(page)
                .size(size)
                .includeHighlights(includeHighlights)
                .build();

        ApiResponse<Map<String, Object>> response = boardItemService.getItemListBySlug(memberId, normalizedSlug,
                searchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 상세 조회
     * GET /boards/{boardId}/items/{itemId}
     */
    @GetMapping("/{boardId:\\d+}/items/{itemId}")
    public ResponseEntity<ApiResponse<BoardItemDto>> getItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Get item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<BoardItemDto> response = boardItemService.getItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 상세 조회 (slug 기반)
     * GET /boards/{slug}/items/{itemId}
     */
    @GetMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}")
    public ResponseEntity<ApiResponse<BoardItemDto>> getItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);
        ApiResponse<BoardItemDto> response = boardItemService.getItemBySlug(memberId, normalizedSlug, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 작성
     * POST /boards/{boardId}/items
     */
    @PostMapping("/{boardId:\\d+}/items")
    public ResponseEntity<ApiResponse<BoardItemDto>> createItem(
            @PathVariable Long boardId,
            @RequestBody BoardItemRequest request) {
        UUID memberId = getMemberId();
        log.info("Create item for board: {}, member: {}", boardId, memberId);

        ApiResponse<BoardItemDto> response = boardItemService.createItem(memberId, boardId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 작성 (slug 기반)
     * POST /boards/{slug}/items
     */
    @PostMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items")
    public ResponseEntity<ApiResponse<BoardItemDto>> createItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @RequestBody BoardItemRequest request) {
        String normalizedSlug = normalizeSlug(slug);

        UUID memberId = getMemberId();
        ApiResponse<BoardItemDto> response = boardItemService.createItemBySlug(memberId, normalizedSlug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정
     * PUT /boards/{boardId}/items/{itemId}
     */
    @PutMapping("/{boardId:\\d+}/items/{itemId}")
    public ResponseEntity<ApiResponse<BoardItemDto>> updateItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @RequestBody BoardItemRequest request) {
        UUID memberId = getMemberId();
        log.info("Update item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<BoardItemDto> response = boardItemService.updateItem(memberId, boardId, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정 (slug 기반)
     * PUT /boards/{slug}/items/{itemId}
     */
    @PutMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}")
    public ResponseEntity<ApiResponse<BoardItemDto>> updateItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId,
            @RequestBody BoardItemRequest request) {
        String normalizedSlug = normalizeSlug(slug);
        UUID memberId = getMemberId();

        ApiResponse<BoardItemDto> response = boardItemService.updateItemBySlug(memberId, normalizedSlug, itemId,
                request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제
     * DELETE /boards/{boardId}/items/{itemId}
     */
    @DeleteMapping("/{boardId:\\d+}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Delete item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Void> response = boardItemService.deleteItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제 (slug 기반)
     * DELETE /boards/{slug}/items/{itemId}
     */
    @DeleteMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);

        ApiResponse<Void> response = boardItemService.deleteItemBySlug(memberId, normalizedSlug, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감
     * POST /boards/{boardId}/items/{itemId}/like
     */
    @PostMapping("/{boardId:\\d+}/items/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> likeItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Like item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Integer> response = boardItemService.likeItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감 (slug 기반)
     * POST /boards/{slug}/items/{itemId}/like
     */
    @PostMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> likeItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);
        ApiResponse<Integer> response = boardItemService.likeItemBySlug(memberId, normalizedSlug, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감 취소
     * DELETE /boards/{boardId}/items/{itemId}/like
     */
    @DeleteMapping("/{boardId:\\d+}/items/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Unlike item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Integer> response = boardItemService.unlikeItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감 취소 (slug 기반)
     * DELETE /boards/{slug}/items/{itemId}/like
     */
    @DeleteMapping("/{slug:(?!\\d+$)(?!items$)[a-z0-9-]+}/items/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeItemBySlug(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        String normalizedSlug = normalizeSlug(slug);
        ApiResponse<Integer> response = boardItemService.unlikeItemBySlug(memberId, normalizedSlug, itemId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }

    private String normalizeSlug(String slug) {
        String normalized = slug.toLowerCase();
        if (RESERVED_SLUGS.contains(normalized)) {
            throw new com.childcare.global.exception.BoardException(
                    com.childcare.global.exception.BoardException.BoardErrorCode.BOARD_SLUG_RESERVED);
        }
        return normalized;
    }
}
