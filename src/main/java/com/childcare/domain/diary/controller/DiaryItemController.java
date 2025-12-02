package com.childcare.domain.diary.controller;

import com.childcare.domain.diary.dto.DiaryItemResponse;
import com.childcare.domain.diary.service.DiaryItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diary-items")
@RequiredArgsConstructor
@Slf4j
public class DiaryItemController {

    private final DiaryItemService diaryItemService;

    @GetMapping
    public ResponseEntity<DiaryItemResponse> getAllItems(@RequestParam(required = false) String division) {
        try {
            log.info("Get diary items request, division: {}", division);

            DiaryItemResponse response;
            if (division != null && !division.isBlank()) {
                response = diaryItemService.getItemsByDivision(division);
            } else {
                response = diaryItemService.getAllItems();
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get diary items", e);
            DiaryItemResponse errorResponse = DiaryItemResponse.builder()
                    .status("error")
                    .code("DIARY_ITEM_999")
                    .message("일지 항목 조회 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
