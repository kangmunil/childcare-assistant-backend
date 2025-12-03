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
        try {
            log.info("Get diaries request for child: {}, date: {}", childId, date);

            ApiResponse<List<ChildDiaryDto>> response;
            if (date != null && !date.isBlank()) {
                response = childDiaryService.getDiariesByChildAndDate(childId, date);
            } else {
                response = childDiaryService.getDiariesByChild(childId);
            }
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to get diaries - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("DIARY_001", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get diaries", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("DIARY_999", "성장일지 조회 실패"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> createDiary(
            @PathVariable Long childId,
            @RequestBody ChildDiaryRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Create diary request for child: {}", childId);

            ApiResponse<List<ChildDiaryDto>> response = childDiaryService.createDiary(memberSeq, childId, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create diary - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("DIARY_002", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create diary", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("DIARY_999", "성장일지 등록 실패: " + e.getMessage()));
        }
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> updateDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId,
            @RequestBody ChildDiaryRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Update diary {} request for child: {}", diaryId, childId);

            ApiResponse<List<ChildDiaryDto>> response = childDiaryService.updateDiary(memberSeq, childId, diaryId, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update diary - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("DIARY_003", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update diary", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("DIARY_999", "성장일지 수정 실패"));
        }
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Delete diary {} request for child: {}", diaryId, childId);

            ApiResponse<Void> response = childDiaryService.deleteDiary(memberSeq, childId, diaryId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to delete diary - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("DIARY_004", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete diary", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("DIARY_999", "성장일지 삭제 실패"));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DiarySummaryDto>> getDailySummary(
            @PathVariable Long childId,
            @RequestParam String date) {
        try {
            log.info("Get daily summary request for child: {}, date: {}", childId, date);

            ApiResponse<DiarySummaryDto> response = childDiaryService.getDailySummary(childId, date);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to get daily summary - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("DIARY_005", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get daily summary", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("DIARY_999", "일지 요약 조회 실패"));
        }
    }

    private Long getMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
