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

import java.util.UUID;

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
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Get my info request for memberId: {}", memberId);

        ApiResponse<MemberDto> response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원정보 수정
     * PUT /members/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto>> updateMyInfo(
            @Valid @RequestBody MemberUpdateRequest request) {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Update my info request for memberId: {}", memberId);

        ApiResponse<MemberDto> response = memberService.updateMyInfo(memberId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 탈퇴
     * DELETE /members/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        UUID memberId = SecurityUtil.getCurrentMemberId();
        log.info("Withdraw request for memberId: {}", memberId);

        ApiResponse<Void> response = memberService.withdraw(memberId);
        return ResponseEntity.ok(response);
    }
}
