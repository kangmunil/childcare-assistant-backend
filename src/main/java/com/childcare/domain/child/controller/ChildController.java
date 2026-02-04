package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildDto;
import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.GrowthHistoryDto;
import com.childcare.domain.child.dto.GrowthHistoryStatDto;
import com.childcare.domain.child.service.ChildService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Slf4j
public class ChildController {

    private final ChildService childService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildDto>>> getChildren() {
        UUID memberId = getMemberId();
        log.info("Get children request for member: {}", memberId);

        ApiResponse<List<ChildDto>> response = childService.getChildrenByMemberId(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChildDto>> getChild(@PathVariable Long id) {
        UUID memberId = getMemberId();
        log.info("Get child {} request for member: {}", id, memberId);

        ApiResponse<ChildDto> response = childService.getChildById(memberId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDto>>> createChild(@RequestBody ChildRequest request) {
        UUID memberId = getMemberId();
        log.info("Create child request for member: {}", memberId);

        ApiResponse<List<ChildDto>> response = childService.createChild(memberId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<List<ChildDto>>> updateChild(@PathVariable Long id, @RequestBody ChildRequest request) {
        UUID memberId = getMemberId();
        log.info("Update child {} request for member: {}", id, memberId);

        ApiResponse<List<ChildDto>> response = childService.updateChild(memberId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChild(@PathVariable Long id) {
        UUID memberId = getMemberId();
        log.info("Delete child {} request for member: {}", id, memberId);

        ApiResponse<Void> response = childService.deleteChild(memberId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<GrowthHistoryDto>>> getGrowthHistory(@PathVariable Long id) {
        UUID memberId = getMemberId();
        log.info("Get growth history for child {} request for member: {}", id, memberId);

        ApiResponse<List<GrowthHistoryDto>> response = childService.getGrowthHistory(memberId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 성장 통계 조회
     * GET /children/{id}/history/stats?type=week&startDate=2025-12-01&endDate=2025-12-07
     * GET /children/{id}/history/stats?type=month&startDate=2025-01-01&endDate=2025-12-31
     * GET /children/{id}/history/stats?type=year&startDate=2024-01-01&endDate=2025-12-31
     */
    @GetMapping("/{id}/history/stats")
    public ResponseEntity<ApiResponse<GrowthHistoryStatDto>> getGrowthHistoryStats(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        UUID memberId = getMemberId();
        log.info("Get growth history stats for child {} request for member: {}, type: {}, from {} to {}", id, memberId, type, startDate, endDate);

        ApiResponse<GrowthHistoryStatDto> response = childService.getGrowthHistoryStats(memberId, id, type, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 가족 관계 삭제
     * DELETE /children/{childId}/parents/{targetMbId}
     */
    @DeleteMapping("/{childId}/parents/{targetMbId}")
    public ResponseEntity<ApiResponse<Void>> deleteParentRelation(
            @PathVariable Long childId,
            @PathVariable UUID targetMbId) {
        UUID memberId = getMemberId();
        log.info("Delete parent relation for child {} member {} by {}", childId, targetMbId, memberId);

        ApiResponse<Void> response = childService.deleteParentRelation(memberId, childId, targetMbId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
