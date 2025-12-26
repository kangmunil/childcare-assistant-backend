package com.childcare.domain.member.service;

import com.childcare.domain.member.dto.MemberDto;
import com.childcare.domain.member.dto.MemberUpdateRequest;
import com.childcare.domain.member.entity.LeaveMember;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.repository.LeaveMemberRepository;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import com.childcare.global.exception.MemberException;
import com.childcare.global.exception.MemberException.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final LeaveMemberRepository leaveMemberRepository;
    private final ParentRepository parentRepository;

    /**
     * 내 정보 조회
     */
    public ApiResponse<MemberDto> getMyInfo(UUID memberId) {
        log.info("Fetching member info for memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        return ApiResponse.success("회원정보 조회 성공", toDto(member));
    }

    /**
     * 회원정보 수정
     */
    @Transactional
    public ApiResponse<MemberDto> updateMyInfo(UUID memberId, MemberUpdateRequest request) {
        log.info("Updating member info for memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        /*
        // 이메일 중복 검사 (다른 회원과 중복되는지)
        if (request.getEmail() != null && !request.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
            }
            member.setEmail(request.getEmail());
        }
        */
        //이메일 변경 불가
        if(!request.getEmail().equals(member.getEmail())) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (request.getName() != null) {
            member.setName(request.getName());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getTel() != null) {
            member.setTel(request.getTel());
        }
        if (request.getPostcode() != null) {
            member.setPostcode(request.getPostcode());
        }
        if (request.getAddr1() != null) {
            member.setAddr1(request.getAddr1());
        }
        if (request.getAddr2() != null) {
            member.setAddr2(request.getAddr2());
        }

        Member updatedMember = memberRepository.save(member);

        return ApiResponse.success("회원정보 수정 성공", toDto(updatedMember));
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public ApiResponse<Void> withdraw(UUID memberId) {
        log.info("Processing withdrawal for memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        // parent 테이블에 내 mb_id가 있으면 탈퇴 불가
        var parentRelations = parentRepository.findByMbId(memberId);
        if (!parentRelations.isEmpty()) {
            throw new MemberException(MemberErrorCode.CHILD_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();

        // member → leave_member 복사
        LeaveMember leaveMember = LeaveMember.builder()
                .id(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .tel(member.getTel())
                .email(member.getEmail())
                .postcode(member.getPostcode())
                .addr1(member.getAddr1())
                .addr2(member.getAddr2())
                .inviteCode(member.getInviteCode())
                .invitedBy(member.getInvitedBy())
                .role(member.getRole())
                .provider(member.getProvider())
                .providerId(member.getProviderId())
                .loginDate(member.getLoginDate())
                .leaveDate(now)
                .createdAt(member.getCreatedAt())
                .build();
        leaveMemberRepository.save(leaveMember);

        // member 삭제
        memberRepository.delete(member);

        log.info("Member {} successfully withdrawn", memberId);
        return ApiResponse.success("회원 탈퇴 성공", null);
    }

    private MemberDto toDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .tel(member.getTel())
                .email(member.getEmail())
                .postcode(member.getPostcode())
                .addr1(member.getAddr1())
                .addr2(member.getAddr2())
                .inviteCode(member.getInviteCode())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }
}
