package com.childcare.domain.auth.service;

import com.childcare.domain.auth.dto.*;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.MemberOAuth;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.domain.member.repository.MemberOAuthRepository;
import com.childcare.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            
            String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail());
            
            return AuthResponse.builder()
                    .status("success")
                    .token(token)
                    .user(AuthResponse.UserDto.builder()
                            .id(member.getMbSeq())
                            .email(member.getEmail())
                            .nickname(getOAuthNickname(member, MemberOAuth.OAuthProvider.KAKAO))
                            .name(member.getName())
                            .build())
                    .build();
                    
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
            
            String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail());
            
            return AuthResponse.builder()
                    .status("success")
                    .token(token)
                    .user(AuthResponse.UserDto.builder()
                            .id(member.getMbSeq())
                            .email(member.getEmail())
                            .nickname(getOAuthNickname(member, MemberOAuth.OAuthProvider.GOOGLE))
                            .name(member.getName())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
    
    public AuthResponse login(String id, String password) {
        try {
            Member member = memberRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            
            if (!passwordEncoder.matches(password, member.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
            
            // Update login date
            member.setLoginDate(LocalDateTime.now());
            memberRepository.save(member);
            
            String token = jwtUtil.generateToken(member.getMbSeq(), member.getEmail());
            
            return AuthResponse.builder()
                    .status("success")
                    .token(token)
                    .user(AuthResponse.UserDto.builder()
                            .id(member.getMbSeq())
                            .email(member.getEmail())
                            .nickname(null) // 일반 로그인은 닉네임 없음
                            .name(member.getName())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }
    
    public AuthResponse register(RegisterRequest request) {
        try {
            if (memberRepository.existsById(request.getId())) {
                throw new RuntimeException("ID already exists");
            }
            
            if (request.getEmail() != null && memberRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
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
                    .build();
            
            Member savedMember = memberRepository.save(member);
            
            String token = jwtUtil.generateToken(savedMember.getMbSeq(), savedMember.getEmail());
            
            return AuthResponse.builder()
                    .status("success")
                    .token(token)
                    .user(AuthResponse.UserDto.builder()
                            .id(savedMember.getMbSeq())
                            .email(savedMember.getEmail())
                            .nickname(null)
                            .name(savedMember.getName())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
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