package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildResponse;
import com.childcare.domain.child.service.ChildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Slf4j
public class ChildController {

    private final ChildService childService;

    @GetMapping("/getList")
    public ResponseEntity<ChildResponse> getChildren() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long memberSeq = (Long) authentication.getPrincipal();

            log.info("Get children request for member: {}", memberSeq);

            ChildResponse response = childService.getChildrenByMemberSeq(memberSeq);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get children", e);
            ChildResponse errorResponse = ChildResponse.builder()
                    .status("error")
                    .message("자녀 목록 조회 실패")
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
