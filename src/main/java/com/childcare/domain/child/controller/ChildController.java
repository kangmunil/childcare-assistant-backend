package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.ChildResponse;
import com.childcare.domain.child.service.ChildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Slf4j
public class ChildController {

    private final ChildService childService;

    @GetMapping
    public ResponseEntity<ChildResponse> getChildren() {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Get children request for member: {}", memberSeq);

            ChildResponse response = childService.getChildrenByMemberSeq(memberSeq);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to get children - validation error", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_001")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to get children", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_999")
                    .message("자녀 목록 조회 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping
    public ResponseEntity<ChildResponse> createChild(@RequestBody ChildRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Create child request for member: {}", memberSeq);

            ChildResponse response = childService.createChild(memberSeq, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create child - validation error", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_002")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to create child", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_999")
                    .message("자녀 등록 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChildResponse> updateChild(@PathVariable Long id, @RequestBody ChildRequest request) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Update child {} request for member: {}", id, memberSeq);

            ChildResponse response = childService.updateChild(memberSeq, id, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update child - validation error", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_003")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to update child", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_999")
                    .message("자녀 정보 수정 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ChildResponse> deleteChild(@PathVariable Long id) {
        try {
            Long memberSeq = getMemberSeq();
            log.info("Delete child {} request for member: {}", id, memberSeq);

            ChildResponse response = childService.deleteChild(memberSeq, id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to delete child - validation error", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_004")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to delete child", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .code("CHILD_999")
                    .message("자녀 정보 삭제 실패")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Long getMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
