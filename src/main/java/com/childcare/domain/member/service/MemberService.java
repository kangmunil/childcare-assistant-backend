package com.childcare.domain.member.service;

import com.childcare.domain.member.dto.MemberDto;
import com.childcare.domain.member.dto.MemberUpdateRequest;
import com.childcare.domain.member.entity.LeaveMember;
import com.childcare.domain.member.entity.LeaveMemberOAuth;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.MemberOAuth;
import com.childcare.domain.member.repository.LeaveMemberOAuthRepository;
import com.childcare.domain.member.repository.LeaveMemberRepository;
import com.childcare.domain.member.repository.MemberOAuthRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberOAuthRepository memberOAuthRepository;
    private final LeaveMemberRepository leaveMemberRepository;
    private final LeaveMemberOAuthRepository leaveMemberOAuthRepository;
    private final ParentRepository parentRepository;

    /**
     * 내 정보 조회
     */
    public ApiResponse<MemberDto> getMyInfo(Long memberSeq) {
        log.info("Fetching member info for memberSeq: {}", memberSeq);

        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        return ApiResponse.success("회원정보 조회 성공", toDto(member));
    }

    /**
     * 회원정보 수정
     */
    @Transactional
    public ApiResponse<MemberDto> updateMyInfo(Long memberSeq, MemberUpdateRequest request) {
        log.info("Updating member info for memberSeq: {}", memberSeq);

        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        // 이메일 중복 검사 (다른 회원과 중복되는지)
        if (request.getEmail() != null && !request.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
            }
            member.setEmail(request.getEmail());
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
    public ApiResponse<Void> withdraw(Long memberSeq) {
        log.info("Processing withdrawal for memberSeq: {}", memberSeq);

        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        // parent 테이블에 내 mb_seq가 있으면 탈퇴 불가
        var parentRelations = parentRepository.findByMbSeq(memberSeq);
        if (!parentRelations.isEmpty()) {
            throw new MemberException(MemberErrorCode.CHILD_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. member_oauth → leave_member_oauth 복사
        List<MemberOAuth> oAuthList = memberOAuthRepository.findByMember(member);
        for (MemberOAuth oAuth : oAuthList) {
            LeaveMemberOAuth leaveOAuth = LeaveMemberOAuth.builder()
                    .oauthSeq(oAuth.getOauthSeq())
                    .mbSeq(member.getMbSeq())
                    .provider(oAuth.getProvider())
                    .providerId(oAuth.getProviderId())
                    .nickname(oAuth.getNickname())
                    .profileImageUrl(oAuth.getProfileImageUrl())
                    .connectDate(oAuth.getConnectedAt())
                    .leaveDate(now)
                    .build();
            leaveMemberOAuthRepository.save(leaveOAuth);
        }

        // 2. member → leave_member 복사
        LeaveMember leaveMember = LeaveMember.builder()
                .mbSeq(member.getMbSeq())
                .id(member.getId())
                .password(member.getPassword())
                .name(member.getName())
                .phone(member.getPhone())
                .tel(member.getTel())
                .email(member.getEmail())
                .postcode(member.getPostcode())
                .addr1(member.getAddr1())
                .addr2(member.getAddr2())
                .inviteCode(member.getInviteCode())
                .invitedBy(member.getInvitedBy())
                .loginDate(member.getLoginDate())
                .leaveDate(now)
                .regDate(member.getRegDate())
                .build();
        leaveMemberRepository.save(leaveMember);

        // 3. member_oauth 삭제
        memberOAuthRepository.deleteAll(oAuthList);

        // 4. member 삭제
        memberRepository.delete(member);

        log.info("Member {} successfully withdrawn", memberSeq);
        return ApiResponse.success("회원 탈퇴 성공", null);
    }

    private MemberDto toDto(Member member) {
        return MemberDto.builder()
                .id(member.getMbSeq())
                .memberId(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .tel(member.getTel())
                .email(member.getEmail())
                .postcode(member.getPostcode())
                .addr1(member.getAddr1())
                .addr2(member.getAddr2())
                .inviteCode(member.getInviteCode())
                .build();
    }
}
