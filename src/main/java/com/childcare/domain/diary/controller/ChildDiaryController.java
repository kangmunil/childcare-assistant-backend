package com.childcare.domain.diary.controller;

import com.childcare.domain.diary.dto.ChildDiaryDto;
import com.childcare.domain.diary.dto.ChildDiaryRequest;
import com.childcare.domain.diary.dto.DiarySummaryDto;
import com.childcare.domain.diary.service.ChildDiaryService;
import com.childcare.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/children/{childId}/diaries")
@RequiredArgsConstructor
@Slf4j
public class ChildDiaryController {

    private final ChildDiaryService childDiaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> getDiaries(
            @PathVariable Long childId,
            @RequestParam(required = false) String date) {
        Long memberSeq = getMemberSeq();
        log.info("Get diaries request for child: {}, date: {}", childId, date);

        ApiResponse<List<ChildDiaryDto>> response;
        if (date != null && !date.isBlank()) {
            response = childDiaryService.getDiariesByChildAndDate(memberSeq, childId, date);
        } else {
            response = childDiaryService.getDiariesByChild(memberSeq, childId);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> createDiary(
            @PathVariable Long childId,
            @RequestBody ChildDiaryRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Create diary request for child: {}", childId);

        ApiResponse<List<ChildDiaryDto>> response = childDiaryService.createDiary(memberSeq, childId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> updateDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId,
            @RequestBody ChildDiaryRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Update diary {} request for child: {}", diaryId, childId);

        ApiResponse<List<ChildDiaryDto>> response = childDiaryService.updateDiary(memberSeq, childId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId) {
        Long memberSeq = getMemberSeq();
        log.info("Delete diary {} request for child: {}", diaryId, childId);

        ApiResponse<Void> response = childDiaryService.deleteDiary(memberSeq, childId, diaryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DiarySummaryDto>> getDailySummary(
            @PathVariable Long childId,
            @RequestParam String date) {
        Long memberSeq = getMemberSeq();
        log.info("Get daily summary request for child: {}, date: {}", childId, date);

        ApiResponse<DiarySummaryDto> response = childDiaryService.getDailySummary(memberSeq, childId, date);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
