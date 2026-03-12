package com.childcare.domain.diary.controller;

import com.childcare.domain.diary.dto.ChildDiaryDto;
import com.childcare.domain.diary.dto.ChildDiaryRequest;
import com.childcare.domain.diary.dto.DiaryMemoDto;
import com.childcare.domain.diary.dto.DiaryMemoRequest;
import com.childcare.domain.diary.dto.DiaryStatDto;
import com.childcare.domain.diary.dto.DiarySummaryDto;
import com.childcare.domain.diary.service.ChildDiaryService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        UUID memberId = getMemberId();
        log.info("Get diaries request for child: {}, date: {}", childId, date);

        ApiResponse<List<ChildDiaryDto>> response;
        if (date != null && !date.isBlank()) {
            response = childDiaryService.getDiariesByChildAndDate(memberId, childId, date);
        } else {
            response = childDiaryService.getDiariesByChild(memberId, childId);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> createDiary(
            @PathVariable Long childId,
            @RequestBody ChildDiaryRequest request) {
        UUID memberId = getMemberId();
        log.info("Create diary request for child: {}", childId);

        ApiResponse<List<ChildDiaryDto>> response = childDiaryService.createDiary(memberId, childId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<List<ChildDiaryDto>>> updateDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId,
            @RequestBody ChildDiaryRequest request) {
        UUID memberId = getMemberId();
        log.info("Update diary {} request for child: {}", diaryId, childId);

        ApiResponse<List<ChildDiaryDto>> response = childDiaryService.updateDiary(memberId, childId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId) {
        UUID memberId = getMemberId();
        log.info("Delete diary {} request for child: {}", diaryId, childId);

        ApiResponse<Void> response = childDiaryService.deleteDiary(memberId, childId, diaryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DiarySummaryDto>> getDailySummary(
            @PathVariable Long childId,
            @RequestParam String date) {
        UUID memberId = getMemberId();
        log.info("Get daily summary request for child: {}, date: {}", childId, date);

        ApiResponse<DiarySummaryDto> response = childDiaryService.getDailySummary(memberId, childId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 일지 통계 조회
     * GET /children/{childId}/diaries/stats?type=week&startDate=2025-12-01&endDate=2025-12-07
     * GET /children/{childId}/diaries/stats?type=month&startDate=2025-12-01&endDate=2025-12-31
     * GET /children/{childId}/diaries/stats?type=year&startDate=2025-01-01&endDate=2025-12-31
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DiaryStatDto>> getDiaryStats(
            @PathVariable Long childId,
            @RequestParam String type,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        UUID memberId = getMemberId();
        log.info("Get diary stats request for child: {}, type: {}, from {} to {}", childId, type, startDate, endDate);

        ApiResponse<DiaryStatDto> response = childDiaryService.getDiaryStats(memberId, childId, type, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    // ==================== 메모 관련 API ====================

    /**
     * 특정 날짜의 메모 조회
     * GET /children/{childId}/diaries/memo?date=2026-02-04
     */
    @GetMapping("/memo")
    public ResponseEntity<ApiResponse<DiaryMemoDto>> getMemo(
            @PathVariable Long childId,
            @RequestParam String date) {
        UUID memberId = getMemberId();
        log.info("Get memo request for child: {}, date: {}", childId, date);

        ApiResponse<DiaryMemoDto> response = childDiaryService.getMemo(memberId, childId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모 생성
     * POST /children/{childId}/diaries/memo
     */
    @PostMapping("/memo")
    public ResponseEntity<ApiResponse<DiaryMemoDto>> createMemo(
            @PathVariable Long childId,
            @RequestBody DiaryMemoRequest request) {
        UUID memberId = getMemberId();
        log.info("Create memo request for child: {}, date: {}", childId, request.getDate());

        ApiResponse<DiaryMemoDto> response = childDiaryService.createMemo(memberId, childId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모 수정
     * PUT /children/{childId}/diaries/memo/{memoId}
     */
    @PutMapping("/memo/{memoId}")
    public ResponseEntity<ApiResponse<DiaryMemoDto>> updateMemo(
            @PathVariable Long childId,
            @PathVariable Long memoId,
            @RequestBody DiaryMemoRequest request) {
        UUID memberId = getMemberId();
        log.info("Update memo {} request for child: {}", memoId, childId);

        ApiResponse<DiaryMemoDto> response = childDiaryService.updateMemo(memberId, childId, memoId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모 삭제
     * DELETE /children/{childId}/diaries/memo/{memoId}
     */
    @DeleteMapping("/memo/{memoId}")
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @PathVariable Long childId,
            @PathVariable Long memoId) {
        UUID memberId = getMemberId();
        log.info("Delete memo {} request for child: {}", memoId, childId);

        ApiResponse<Void> response = childDiaryService.deleteMemo(memberId, childId, memoId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
