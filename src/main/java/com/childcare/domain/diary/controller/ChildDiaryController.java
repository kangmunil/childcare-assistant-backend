package com.childcare.domain.diary.controller;

import com.childcare.domain.diary.dto.ChildDiaryRequest;
import com.childcare.domain.diary.dto.ChildDiaryResponse;
import com.childcare.domain.diary.service.ChildDiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/children/{childId}/diaries")
@RequiredArgsConstructor
@Slf4j
public class ChildDiaryController {

    private final ChildDiaryService childDiaryService;

    @GetMapping
    public ResponseEntity<ChildDiaryResponse> getDiaries(
            @PathVariable Long childId,
            @RequestParam(required = false) String date) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Get diaries request for child: {}, date: {}", childId, date);

            ChildDiaryResponse response;
            if (date != null && !date.isBlank()) {
                response = childDiaryService.getDiariesByChildAndDate(childId, date);
            } else {
                response = childDiaryService.getDiariesByChild(childId);
            }
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to get diaries - validation error", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_001")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to get diaries", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_999")
                    .message("성장일지 조회 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping
    public ResponseEntity<ChildDiaryResponse> createDiary(
            @PathVariable Long childId,
            @RequestBody ChildDiaryRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Create diary request for child: {}", childId);

            ChildDiaryResponse response = childDiaryService.createDiary(memberSeq, childId, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create diary - validation error", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_002")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to create diary", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_999")
                    .message("성장일지 등록 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<ChildDiaryResponse> updateDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId,
            @RequestBody ChildDiaryRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Update diary {} request for child: {}", diaryId, childId);

            ChildDiaryResponse response = childDiaryService.updateDiary(memberSeq, childId, diaryId, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update diary - validation error", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_003")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to update diary", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_999")
                    .message("성장일지 수정 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ChildDiaryResponse> deleteDiary(
            @PathVariable Long childId,
            @PathVariable Long diaryId) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Delete diary {} request for child: {}", diaryId, childId);

            ChildDiaryResponse response = childDiaryService.deleteDiary(memberSeq, childId, diaryId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to delete diary - validation error", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_004")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to delete diary", e);
            ChildDiaryResponse errorResponse = ChildDiaryResponse.builder()
                    .status("error")
                    .code("DIARY_999")
                    .message("성장일지 삭제 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Long getMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
