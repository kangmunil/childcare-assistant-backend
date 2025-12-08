package com.childcare.domain.member.controller;

import com.childcare.domain.member.dto.MemberDto;
import com.childcare.domain.member.dto.MemberUpdateRequest;
import com.childcare.domain.member.service.MemberService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    /**
     * 내 정보 조회
     * GET /members/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto>> getMyInfo() {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        log.info("Get my info request for memberSeq: {}", memberSeq);

        ApiResponse<MemberDto> response = memberService.getMyInfo(memberSeq);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원정보 수정
     * PUT /members/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto>> updateMyInfo(
            @Valid @RequestBody MemberUpdateRequest request) {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        log.info("Update my info request for memberSeq: {}", memberSeq);

        ApiResponse<MemberDto> response = memberService.updateMyInfo(memberSeq, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 탈퇴
     * DELETE /members/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        Long memberSeq = SecurityUtil.getCurrentMemberSeq();
        log.info("Withdraw request for memberSeq: {}", memberSeq);

        ApiResponse<Void> response = memberService.withdraw(memberSeq);
        return ResponseEntity.ok(response);
    }
}
