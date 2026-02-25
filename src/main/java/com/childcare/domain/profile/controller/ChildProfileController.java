package com.childcare.domain.profile.controller;

import com.childcare.domain.profile.dto.ChildProfileDto;
import com.childcare.domain.profile.dto.ChildProfilePatchRequest;
import com.childcare.domain.profile.dto.ChildProfileSummaryDto;
import com.childcare.domain.profile.service.ChildProfileService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/children/{childId}/profile")
@RequiredArgsConstructor
@Slf4j
public class ChildProfileController {

    private final ChildProfileService childProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChildProfileDto>> getProfile(
            @PathVariable Long childId,
            @RequestParam(value = "sections", required = false) String sections
    ) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Get child profile request. memberId={}, childId={}, sections={}", memberId, childId, sections);

        ApiResponse<ChildProfileDto> response = childProfileService.getProfile(memberId, childId, sections);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ChildProfileDto>> patchProfile(
            @PathVariable Long childId,
            @RequestBody ChildProfilePatchRequest request
    ) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Patch child profile request. memberId={}, childId={}", memberId, childId);

        ApiResponse<ChildProfileDto> response = childProfileService.patchProfile(memberId, childId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ChildProfileSummaryDto>> getSummary(@PathVariable Long childId) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Get child profile summary request. memberId={}, childId={}", memberId, childId);

        ApiResponse<ChildProfileSummaryDto> response = childProfileService.getSummary(memberId, childId);
        return ResponseEntity.ok(response);
    }
}
