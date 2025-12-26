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
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/boards/{boardId}/items")
@RequiredArgsConstructor
@Slf4j
public class BoardItemController {

    private final BoardItemService boardItemService;

    /**
     * 게시글 목록 조회
     * GET /boards/{boardId}/items?searchType=title&keyword=검색어&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItemList(
            @PathVariable Long boardId,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID memberId = getMemberId();
        log.info("Get item list for board: {}, member: {}", boardId, memberId);

        BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                .searchType(searchType)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        ApiResponse<Map<String, Object>> response = boardItemService.getItemList(memberId, boardId, searchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 상세 조회
     * GET /boards/{boardId}/items/{itemId}
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<BoardItemDto>> getItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Get item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<BoardItemDto> response = boardItemService.getItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 작성
     * POST /boards/{boardId}/items
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BoardItemDto>> createItem(
            @PathVariable Long boardId,
            @RequestBody BoardItemRequest request) {
        UUID memberId = getMemberId();
        log.info("Create item for board: {}, member: {}", boardId, memberId);

        ApiResponse<BoardItemDto> response = boardItemService.createItem(memberId, boardId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정
     * PUT /boards/{boardId}/items/{itemId}
     */
    @PutMapping("/{itemId}")
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
     * 게시글 삭제
     * DELETE /boards/{boardId}/items/{itemId}
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Delete item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Void> response = boardItemService.deleteItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감
     * POST /boards/{boardId}/items/{itemId}/like
     */
    @PostMapping("/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> likeItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Like item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Integer> response = boardItemService.likeItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 공감 취소
     * DELETE /boards/{boardId}/items/{itemId}/like
     */
    @DeleteMapping("/{itemId}/like")
    public ResponseEntity<ApiResponse<Integer>> unlikeItem(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        UUID memberId = getMemberId();
        log.info("Unlike item: {} for board: {}, member: {}", itemId, boardId, memberId);

        ApiResponse<Integer> response = boardItemService.unlikeItem(memberId, boardId, itemId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
