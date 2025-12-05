package com.childcare.domain.auth.service;

import com.childcare.domain.auth.dto.*;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.MemberOAuth;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.domain.member.repository.MemberOAuthRepository;
import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import com.childcare.global.util.InviteCodeGenerator;
import com.childcare.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberOAuthRepository memberOAuthRepository;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;
    private final PasswordEncoder passwordEncoder;
    private final InviteCodeGenerator inviteCodeGenerator;

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
                    MemberOAuth.OAuthProvider.KAKAO,
                    kakaoUserInfo.getId().toString(),
                    kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl()
            );

            String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
            String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail(), role);

            return buildAuthResponse("카카오 로그인 성공", token, member, getOAuthNickname(member, MemberOAuth.OAuthProvider.KAKAO));

        } catch (Exception e) {
            log.error("Kakao authentication failed", e);
            throw new RuntimeException("Kakao authentication failed: " + e.getMessage());
        }
    }

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
                    MemberOAuth.OAuthProvider.GOOGLE,
                    googleUserInfo.getSub(),
                    googleUserInfo.getPicture()
            );

            String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
            String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail(), role);

            return buildAuthResponse("구글 로그인 성공", token, member, getOAuthNickname(member, MemberOAuth.OAuthProvider.GOOGLE));

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    public AuthResponse login(String id, String password) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // Update login date
        member.setLoginDate(LocalDateTime.now());
        memberRepository.save(member);

        String role = member.getRole() != null ? member.getRole().name() : Role.USER.name();
        String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail(), role);

        return buildAuthResponse("로그인 성공", token, member, null);
    }

    public AuthResponse register(RegisterRequest request) {
        if (memberRepository.existsById(request.getId())) {
            throw new AuthException(AuthErrorCode.ID_ALREADY_EXISTS);
        }

        if (request.getEmail() != null && memberRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 추천인 코드 검증 (선택)
        Long invitedBy = null;
        if (StringUtils.hasText(request.getReferralCode())) {
            Member referrer = memberRepository.findByInviteCode(request.getReferralCode())
                    .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFERRAL_CODE));
            invitedBy = referrer.getMbSeq();
        }

        Member member = Member.builder()
                .id(request.getId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .tel(request.getTel())
                .addr1(request.getAddr1())
                .addr2(request.getAddr2())
                .addr3(request.getAddr3())
                .invitedBy(invitedBy)
                .inviteCode(inviteCodeGenerator.generate())
                .build();

        Member savedMember = memberRepository.save(member);

        String role = savedMember.getRole() != null ? savedMember.getRole().name() : Role.USER.name();
        String token = jwtUtil.generateToken(savedMember.getMbSeq(), savedMember.getEmail(), role);

        return buildAuthResponse("회원가입 성공", token, savedMember, null);
    }

    private AuthResponse buildAuthResponse(String message, String token, Member member, String nickname) {
        return AuthResponse.builder()
                .status("success")
                .message(message)
                .data(AuthResponse.AuthData.builder()
                        .accessToken(token)
                        .refreshToken(null)
                        .user(AuthResponse.UserDto.builder()
                                .id(member.getMbSeq())
                                .email(member.getEmail())
                                .nickname(nickname)
                                .name(member.getName())
                                .build())
                        .build())
                .build();
    }

    private Member findOrCreateOAuthUser(String email, String nickname, MemberOAuth.OAuthProvider provider, String providerId, String profileImageUrl) {
        // 먼저 기존 OAuth 연동 확인
        return memberOAuthRepository.findByProviderAndProviderId(provider, providerId)
                .map(MemberOAuth::getMember)
                .orElseGet(() -> {
                    // 이메일로 기존 회원 찾기
                    Member member = memberRepository.findByEmail(email)
                            .orElseGet(() -> {
                                // 새로운 회원 생성
                                Member newMember = Member.builder()
                                        .email(email)
                                        .name(nickname) // OAuth는 닉네임을 name으로 사용
                                        .inviteCode(inviteCodeGenerator.generate())
                                        .build();
                                return memberRepository.save(newMember);
                            });

                    // OAuth 연동 정보 저장
                    MemberOAuth oAuth = MemberOAuth.builder()
                            .member(member)
                            .provider(provider)
                            .providerId(providerId)
                            .nickname(nickname)
                            .profileImageUrl(profileImageUrl)
                            .build();
                    memberOAuthRepository.save(oAuth);

                    return member;
                });
    }

    private String getOAuthNickname(Member member, MemberOAuth.OAuthProvider provider) {
        return member.getOAuthConnections().stream()
                .filter(oauth -> oauth.getProvider() == provider)
                .findFirst()
                .map(MemberOAuth::getNickname)
                .orElse(null);
    }
}
