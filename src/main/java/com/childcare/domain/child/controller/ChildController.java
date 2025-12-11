package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildDto;
import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.GrowthHistoryDto;
import com.childcare.domain.child.service.ChildService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Slf4j
public class ChildController {

    private final ChildService childService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildDto>>> getChildren() {
        Long memberSeq = getMemberSeq();
        log.info("Get children request for member: {}", memberSeq);

        ApiResponse<List<ChildDto>> response = childService.getChildrenByMemberSeq(memberSeq);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChildDto>> getChild(@PathVariable Long id) {
        Long memberSeq = getMemberSeq();
        log.info("Get child {} request for member: {}", id, memberSeq);

        ApiResponse<ChildDto> response = childService.getChildById(memberSeq, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDto>>> createChild(@RequestBody ChildRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Create child request for member: {}", memberSeq);

        ApiResponse<List<ChildDto>> response = childService.createChild(memberSeq, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<List<ChildDto>>> updateChild(@PathVariable Long id, @RequestBody ChildRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Update child {} request for member: {}", id, memberSeq);

        ApiResponse<List<ChildDto>> response = childService.updateChild(memberSeq, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChild(@PathVariable Long id) {
        Long memberSeq = getMemberSeq();
        log.info("Delete child {} request for member: {}", id, memberSeq);

        ApiResponse<Void> response = childService.deleteChild(memberSeq, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<GrowthHistoryDto>>> getGrowthHistory(@PathVariable Long id) {
        Long memberSeq = getMemberSeq();
        log.info("Get growth history for child {} request for member: {}", id, memberSeq);

        ApiResponse<List<GrowthHistoryDto>> response = childService.getGrowthHistory(memberSeq, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 가족 관계 삭제
     * DELETE /children/{childId}/parents/{targetMbSeq}
     */
    @DeleteMapping("/{childId}/parents/{targetMbSeq}")
    public ResponseEntity<ApiResponse<Void>> deleteParentRelation(
            @PathVariable Long childId,
            @PathVariable Long targetMbSeq) {
        Long memberSeq = getMemberSeq();
        log.info("Delete parent relation for child {} member {} by {}", childId, targetMbSeq, memberSeq);

        ApiResponse<Void> response = childService.deleteParentRelation(memberSeq, childId, targetMbSeq);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        return SecurityUtil.getCurrentMemberSeq();
    }
}
