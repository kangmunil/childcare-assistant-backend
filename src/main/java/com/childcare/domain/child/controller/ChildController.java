package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildDto;
import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.service.ChildService;
import com.childcare.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        try {
            Long memberSeq = getMemberSeq();
            log.info("Get children request for member: {}", memberSeq);

            ApiResponse<List<ChildDto>> response = childService.getChildrenByMemberSeq(memberSeq);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to get children - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("CHILD_001", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get children", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("CHILD_999", "자녀 목록 조회 실패"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ChildDto>>> createChild(@RequestBody ChildRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Create child request for member: {}", memberSeq);

            ApiResponse<List<ChildDto>> response = childService.createChild(memberSeq, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create child - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("CHILD_002", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create child", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("CHILD_999", "자녀 등록 실패"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<List<ChildDto>>> updateChild(@PathVariable Long id, @RequestBody ChildRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Update child {} request for member: {}", id, memberSeq);

            ApiResponse<List<ChildDto>> response = childService.updateChild(memberSeq, id, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update child - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("CHILD_003", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update child", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("CHILD_999", "자녀 정보 수정 실패"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChild(@PathVariable Long id) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Delete child {} request for member: {}", id, memberSeq);

            ApiResponse<Void> response = childService.deleteChild(memberSeq, id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to delete child - validation error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("CHILD_004", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete child", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("CHILD_999", "자녀 정보 삭제 실패"));
        }
    }

    private Long getMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
