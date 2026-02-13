package com.childcare.domain.family.controller;

import com.childcare.domain.family.dto.FamilyMemberDto;
import com.childcare.domain.family.dto.FamilyShareRequest;
import com.childcare.domain.family.dto.RelationUpdateRequest;
import com.childcare.domain.family.service.FamilyService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/children/{childId}/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * 자녀별 공유 가족 목록 조회
     * GET /children/{childId}/family
     */
    @GetMapping
    public ApiResponse<List<FamilyMemberDto>> getFamilyMembers(@PathVariable Long childId) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        return familyService.getFamilyMembers(memberId, childId);
    }

    /**
     * 가족 공유 추가 (초대코드 활용)
     * POST /children/{childId}/family
     */
    @PostMapping
    public ApiResponse<FamilyMemberDto> addFamilyMember(
            @PathVariable Long childId,
            @RequestBody FamilyShareRequest request) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        return familyService.addFamilyMember(memberId, childId, request);
    }

    /**
     * 가족 공유 해제
     * DELETE /children/{childId}/family/{memberId}
     */
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> removeFamilyMember(
            @PathVariable Long childId,
            @PathVariable UUID memberId) {
        UUID myMemberId = SecurityUtil.getCurrentMemberId();
        return familyService.removeFamilyMember(myMemberId, childId, memberId);
    }

    /**
     * 초대 승인
     * PUT /children/{childId}/family/{memberId}/approve
     */
    @PutMapping("/{memberId}/approve")
    public ApiResponse<FamilyMemberDto> approveInvitation(
            @PathVariable Long childId,
            @PathVariable UUID memberId) {
        UUID myMemberId = SecurityUtil.getCurrentMemberId();
        return familyService.approveInvitation(myMemberId, childId, memberId);
    }

    /**
     * 초대 거절
     * DELETE /children/{childId}/family/{memberId}/reject
     */
    @DeleteMapping("/{memberId}/reject")
    public ApiResponse<Void> rejectInvitation(
            @PathVariable Long childId,
            @PathVariable UUID memberId) {
        UUID myMemberId = SecurityUtil.getCurrentMemberId();
        return familyService.rejectInvitation(myMemberId, childId, memberId);
    }

    /**
     * 관계명 수정 (아빠, 엄마, 할머니 등)
     * PUT /children/{childId}/family/{memberId}/relation
     */
    @PutMapping("/{memberId}/relation")
    public ApiResponse<FamilyMemberDto> updateRelation(
            @PathVariable Long childId,
            @PathVariable UUID memberId,
            @RequestBody RelationUpdateRequest request) {
        UUID myMemberId = SecurityUtil.getCurrentMemberId();
        return familyService.updateRelation(myMemberId, childId, memberId, request);
    }
}
