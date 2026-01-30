package com.childcare.domain.growth.controller;

import com.childcare.domain.growth.dto.GrowthHistoryRequest;
import com.childcare.domain.growth.dto.GrowthHistoryResponse;
import com.childcare.domain.growth.service.GrowthHistoryService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/children/{childId}/history")
@RequiredArgsConstructor
@Slf4j
public class GrowthHistoryController {

    private final GrowthHistoryService growthHistoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<GrowthHistoryResponse>> createHistory(
            @PathVariable Long childId,
            @RequestBody GrowthHistoryRequest request) {
        UUID memberId = getMemberId();
        log.info("Create growth history request for child: {}", childId);

        GrowthHistoryResponse response = growthHistoryService.createHistory(memberId, childId, request);
        return ResponseEntity.ok(ApiResponse.success("성장 이력 등록 성공", response));
    }

    @PutMapping("/{historyId}")
    public ResponseEntity<ApiResponse<GrowthHistoryResponse>> updateHistory(
            @PathVariable Long childId,
            @PathVariable Long historyId,
            @RequestBody GrowthHistoryRequest request) {
        UUID memberId = getMemberId();
        log.info("Update growth history {} request for child: {}", historyId, childId);

        GrowthHistoryResponse response = growthHistoryService.updateHistory(memberId, childId, historyId, request);
        return ResponseEntity.ok(ApiResponse.success("성장 이력 수정 성공", response));
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @PathVariable Long childId,
            @PathVariable Long historyId) {
        UUID memberId = getMemberId();
        log.info("Delete growth history {} request for child: {}", historyId, childId);

        growthHistoryService.deleteHistory(memberId, childId, historyId);
        return ResponseEntity.ok(ApiResponse.success("성장 이력 삭제 성공", null));
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
