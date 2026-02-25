package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardDto;
import com.childcare.domain.board.dto.BoardSearchRequest;
import com.childcare.domain.board.service.BoardService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardDto>>> getBoards() {
        log.info("Get boards request");
        return ResponseEntity.ok(boardService.getBoards());
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllItems(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Get all items request for member: {}", memberId);

        BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                .searchType(searchType)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(boardService.getAllItems(memberId, searchRequest));
    }
}
