package com.childcare.domain.auth.service;

import com.childcare.domain.auth.dto.*;
import com.childcare.domain.auth.entity.RefreshToken;
import com.childcare.domain.auth.repository.RefreshTokenRepository;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import com.childcare.global.util.InviteCodeGenerator;
import com.childcare.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final ParentRepository parentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;
    private final InviteCodeGenerator inviteCodeGenerator;

    /**
     * 카카오 OAuth 로그인
     */
    public AuthResponse authenticateKakao(String accessToken) {
        try {
            KakaoUserInfo kakaoUserInfo = webClient
                    .get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();

            if (kakaoUserInfo == null) {
                throw new RuntimeException("Failed to get user info from Kakao");
            }

            Member member = findOrCreateOAuthUser(
                    kakaoUserInfo.getKakaoAccount().getEmail(),
                    kakaoUserInfo.getKakaoAccount().getProfile().getNickname(),
                    "KAKAO",
                    kakaoUserInfo.getId().toString(),
                    kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl()
            );

            String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
            String token = jwtUtil.generateToken(member.getId(), member.getEmail(), role);

            return buildAuthResponse("카카오 로그인 성공", token, member);

        } catch (Exception e) {
            log.error("Kakao authentication failed", e);
            throw new RuntimeException("Kakao authentication failed: " + e.getMessage());
        }
    }

    /**
     * 구글 OAuth 로그인
     */
    public AuthResponse authenticateGoogle(String accessToken) {
        try {
            GoogleUserInfo googleUserInfo = webClient
                    .get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();

            if (googleUserInfo == null) {
                throw new RuntimeException("Failed to get user info from Google");
            }

            Member member = findOrCreateOAuthUser(
                    googleUserInfo.getEmail(),
                    googleUserInfo.getName(),
                    "GOOGLE",
                    googleUserInfo.getSub(),
                    googleUserInfo.getPicture()
            );

            String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
            String token = jwtUtil.generateToken(member.getId(), member.getEmail(), role);

            return buildAuthResponse("구글 로그인 성공", token, member);

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * 테스트용 회원 생성 및 로그인 (OAuth API 없이)
     */
    public AuthResponse testRegister(String email, String name) {
        // 기존 회원 확인 (이메일로)
        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) {
            // 새 회원 생성
            member = Member.builder()
                    .email(email)
                    .name(name)
                    .inviteCode(inviteCodeGenerator.generate())
                    .role(Role.USER)
                    .provider("TEST")
                    .providerId(UUID.randomUUID().toString())
                    .build();
            member = memberRepository.save(member);
            log.info("Created new test member: {}", member.getId());
        }

        // JWT 토큰 생성
        String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
        String token = jwtUtil.generateToken(member.getId(), member.getEmail(), role);

        return buildAuthResponse("테스트 로그인 성공", token, member);
    }

    /**
     * OAuth 사용자 조회 또는 생성
     */
    private Member findOrCreateOAuthUser(String email, String nickname, String provider, String providerId, String profileImageUrl) {
        // 먼저 provider + providerId로 기존 회원 확인
        return memberRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 이메일로 기존 회원 찾기
                    Member existingMember = memberRepository.findByEmail(email).orElse(null);

                    if (existingMember != null) {
                        // 기존 회원에 OAuth 정보 업데이트
                        existingMember.setProvider(provider);
                        existingMember.setProviderId(providerId);
                        existingMember.setProfileImageUrl(profileImageUrl);
                        existingMember.setLoginDate(LocalDateTime.now());
                        return memberRepository.save(existingMember);
                    }

                    // 새로운 회원 생성
                    Member newMember = Member.builder()
                            .email(email)
                            .name(nickname)
                            .provider(provider)
                            .providerId(providerId)
                            .profileImageUrl(profileImageUrl)
                            .inviteCode(inviteCodeGenerator.generate())
                            .role(Role.USER)
                            .build();
                    return memberRepository.save(newMember);
                });
    }

    /**
     * 초대코드로 가입 시 자녀 공유
     */
    private void shareChildrenWithInvitee(UUID referrerMbId, UUID inviteeMbId) {
        List<Parent> referrerChildren = parentRepository.findByMbId(referrerMbId);

        for (Parent referrerParent : referrerChildren) {
            Parent sharedParent = Parent.builder()
                    .mbId(inviteeMbId)
                    .chSeq(referrerParent.getChSeq())
                    .relation("family")
                    .authManage("0")
                    .authRead("1")
                    .authWrite("1")
                    .authDelete("1")
                    .regId(referrerMbId)
                    .regDate(LocalDateTime.now())
                    .build();

            parentRepository.save(sharedParent);
            log.info("Shared child {} with invitee {}", referrerParent.getChSeq(), inviteeMbId);
        }
    }

    private AuthResponse buildAuthResponse(String message, String accessToken, Member member) {
        // 기존 refresh token 무효화
        refreshTokenRepository.revokeAllByMbId(member.getId(), LocalDateTime.now());

        // 새 refresh token 생성
        String refreshTokenValue = jwtUtil.generateRefreshToken(member.getId());
        long refreshTokenValidityMs = jwtUtil.getRefreshTokenValidityInMilliseconds();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValidityMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .mbId(member.getId())
                .token(refreshTokenValue)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token created for member: {}", member.getId());

        return AuthResponse.builder()
                .status("success")
                .message(message)
                .data(AuthResponse.AuthData.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenValue)
                        .user(AuthResponse.UserDto.builder()
                                .id(member.getId())
                                .email(member.getEmail())
                                .nickname(member.getName())
                                .name(member.getName())
                                .profileImageUrl(member.getProfileImageUrl())
                                .build())
                        .build())
                .build();
    }

    /**
     * Refresh Token으로 새 Access Token 발급
     */
    public AuthResponse refresh(String refreshTokenValue) {
        // Refresh token 검증 (JWT 서명)
        if (!jwtUtil.validateRefreshToken(refreshTokenValue)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        // DB에서 refresh token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

        // 유효성 검사 (만료, 폐기 여부)
        if (!refreshToken.isValid()) {
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        }

        // 회원 조회
        UUID userId = refreshToken.getMbId();
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

        // 새 Access Token 생성
        String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
        String newAccessToken = jwtUtil.generateToken(member.getId(), member.getEmail(), role);

        log.info("Access token refreshed for member: {}", member.getId());

        return AuthResponse.builder()
                .status("success")
                .message("토큰 갱신 성공")
                .data(AuthResponse.AuthData.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshTokenValue)  // 기존 refresh token 유지
                        .user(AuthResponse.UserDto.builder()
                                .id(member.getId())
                                .email(member.getEmail())
                                .nickname(member.getName())
                                .name(member.getName())
                                .profileImageUrl(member.getProfileImageUrl())
                                .build())
                        .build())
                .build();
    }

    /**
     * 로그아웃 (Refresh Token 무효화)
     */
    public void logout(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByMbId(userId, LocalDateTime.now());
        log.info("Logged out member: {}, revoked {} refresh tokens", userId, revokedCount);
    }

    /**
     * Refresh Token으로 로그아웃
     */
    public void logoutByRefreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElse(null);

        if (refreshToken != null && refreshToken.isValid()) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked for member: {}", refreshToken.getMbId());
        }
    }
}
