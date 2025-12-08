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
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        return familyService.getFamilyMembers(memberSeq, childId);
    }

    /**
     * 가족 공유 추가 (초대코드 활용)
     * POST /children/{childId}/family
     */
    @PostMapping
    public ApiResponse<FamilyMemberDto> addFamilyMember(
            @PathVariable Long childId,
            @RequestBody FamilyShareRequest request) {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        return familyService.addFamilyMember(memberSeq, childId, request);
    }

    /**
     * 가족 공유 해제
     * DELETE /children/{childId}/family/{memberId}
     */
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> removeFamilyMember(
            @PathVariable Long childId,
            @PathVariable Long memberId) {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        return familyService.removeFamilyMember(memberSeq, childId, memberId);
    }

    /**
     * 관계명 수정 (아빠, 엄마, 할머니 등)
     * PUT /children/{childId}/family/{memberId}/relation
     */
    @PutMapping("/{memberId}/relation")
    public ApiResponse<FamilyMemberDto> updateRelation(
            @PathVariable Long childId,
            @PathVariable Long memberId,
            @RequestBody RelationUpdateRequest request) {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        return familyService.updateRelation(memberSeq, childId, memberId, request);
    }
}
