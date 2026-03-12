package com.childcare.domain.diary.controller;

import com.childcare.domain.diary.dto.DiaryItemDto;
import com.childcare.domain.diary.service.DiaryItemService;
import com.childcare.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/diary-items")
@RequiredArgsConstructor
@Slf4j
public class DiaryItemController {

    private final DiaryItemService diaryItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiaryItemDto>>> getAllItems(@RequestParam(required = false) String division) {
        log.info("Get diary items request, division: {}", division);

        ApiResponse<List<DiaryItemDto>> response;
        if (division != null && !division.isBlank()) {
            response = diaryItemService.getItemsByDivision(division);
        } else {
            response = diaryItemService.getAllItems();
        }
        return ResponseEntity.ok(response);
    }
}
