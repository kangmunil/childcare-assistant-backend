package com.childcare.domain.family.service;

import com.childcare.domain.family.dto.FamilyMemberDto;
import com.childcare.domain.family.dto.FamilyShareRequest;
import com.childcare.domain.family.dto.RelationUpdateRequest;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import com.childcare.global.exception.ChildAccessDeniedException;
import com.childcare.global.exception.ChildAccessDeniedException.AccessErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FamilyService {

    private final ParentRepository parentRepository;
    private final MemberRepository memberRepository;
    private final ChildAccessValidator childAccessValidator;

    /**
     * 자녀별 공유 가족 목록 조회
     * auth와 상관없이 parent 테이블로 연결되어있다면 누구나 확인할 수 있음
     */
    public ApiResponse<List<FamilyMemberDto>> getFamilyMembers(UUID memberId, Long childId) {
        log.info("Fetching family members for child: {} by member: {}", childId, memberId);

        // 접근 권한만 확인 (read/write/delete 권한은 상관없음)
        childAccessValidator.validateAccess(memberId, childId);

        List<Parent> parents = parentRepository.findByChSeq(childId);

        List<FamilyMemberDto> familyMembers = parents.stream()
                .map(parent -> {
                    Member member = memberRepository.findById(parent.getMbId()).orElse(null);
                    return FamilyMemberDto.builder()
                            .memberId(parent.getMbId())
                            .memberName(member != null ? member.getName() : "Unknown")
                            .relation(parent.getRelation())
                            .authManage(parent.getAuthManage())
                            .authRead(parent.getAuthRead())
                            .authWrite(parent.getAuthWrite())
                            .authDelete(parent.getAuthDelete())
                            .isMe(parent.getMbId().equals(memberId))
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.success("가족 목록 조회 성공", familyMembers);
    }

    /**
     * 공유 관계 끊기 (auth_manage 권한 필요)
     */
    @Transactional
    public ApiResponse<Void> removeFamilyMember(UUID memberId, Long childId, UUID targetMemberId) {
        log.info("Removing family member {} from child {} by member {}", targetMemberId, childId, memberId);

        // 관리 권한 확인
        childAccessValidator.validateManageAccess(memberId, childId);

        // 자기 자신은 삭제할 수 없음
        if (memberId.equals(targetMemberId)) {
            throw new ChildAccessDeniedException(AccessErrorCode.NO_MANAGE_PERMISSION);
        }

        // 대상 회원이 해당 자녀에 연결되어 있는지 확인
        Parent targetParent = parentRepository.findByMbIdAndChSeq(targetMemberId, childId)
                .orElseThrow(() -> new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS));

        // 관리 권한이 있는 회원은 삭제할 수 없음 (보호)
        if ("1".equals(targetParent.getAuthManage())) {
            throw new ChildAccessDeniedException(AccessErrorCode.NO_MANAGE_PERMISSION);
        }

        parentRepository.delete(targetParent);

        return ApiResponse.success("가족 공유 해제 성공", null);
    }

    /**
     * 새로운 공유 관계 생성 (auth_manage 권한 필요, 초대코드 활용)
     */
    @Transactional
    public ApiResponse<FamilyMemberDto> addFamilyMember(UUID memberId, Long childId, FamilyShareRequest request) {
        log.info("Adding family member to child {} by member {} with invite code {}", childId, memberId, request.getInviteCode());

        // 관리 권한 확인
        childAccessValidator.validateManageAccess(memberId, childId);

        // 초대코드로 회원 찾기
        Member targetMember = memberRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFERRAL_CODE));

        // 자기 자신에게 공유할 수 없음
        if (memberId.equals(targetMember.getId())) {
            throw new AuthException(AuthErrorCode.INVALID_REFERRAL_CODE);
        }

        // 이미 공유된 회원인지 확인
        if (parentRepository.findByMbIdAndChSeq(targetMember.getId(), childId).isPresent()) {
            throw new AuthException(AuthErrorCode.MEMBER_ALREADY_EXISTS); // 이미 공유됨
        }

        // 새 공유 관계 생성 (auth_manage는 0, 나머지는 1)
        Parent newParent = Parent.builder()
                .mbId(targetMember.getId())
                .chSeq(childId)
                .relation(request.getRelation() != null ? request.getRelation() : "family")
                .authManage("0")
                .authRead("1")
                .authWrite("1")
                .authDelete("1")
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        Parent savedParent = parentRepository.save(newParent);

        FamilyMemberDto result = FamilyMemberDto.builder()
                .memberId(targetMember.getId())
                .memberName(targetMember.getName())
                .relation(savedParent.getRelation())
                .authManage(savedParent.getAuthManage())
                .authRead(savedParent.getAuthRead())
                .authWrite(savedParent.getAuthWrite())
                .authDelete(savedParent.getAuthDelete())
                .isMe(false)
                .build();

        return ApiResponse.success("가족 공유 추가 성공", result);
    }

    /**
     * 관계명 수정 (권한 상관없이 자녀에 접근 가능한 누구나 가능)
     */
    @Transactional
    public ApiResponse<FamilyMemberDto> updateRelation(UUID memberId, Long childId, UUID targetMemberId, RelationUpdateRequest request) {
        log.info("Updating relation for member {} in child {} by member {}", targetMemberId, childId, memberId);

        // 접근 권한만 확인 (read/write/delete/manage 권한은 상관없음)
        childAccessValidator.validateAccess(memberId, childId);

        // 대상 회원의 parent 관계 조회
        Parent targetParent = parentRepository.findByMbIdAndChSeq(targetMemberId, childId)
                .orElseThrow(() -> new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS));

        // 관계명 수정
        targetParent.setRelation(request.getRelation());
        Parent savedParent = parentRepository.save(targetParent);

        Member member = memberRepository.findById(targetMemberId).orElse(null);

        FamilyMemberDto result = FamilyMemberDto.builder()
                .memberId(savedParent.getMbId())
                .memberName(member != null ? member.getName() : "Unknown")
                .relation(savedParent.getRelation())
                .authManage(savedParent.getAuthManage())
                .authRead(savedParent.getAuthRead())
                .authWrite(savedParent.getAuthWrite())
                .authDelete(savedParent.getAuthDelete())
                .isMe(savedParent.getMbId().equals(memberId))
                .build();

        return ApiResponse.success("관계명 수정 성공", result);
    }
}
