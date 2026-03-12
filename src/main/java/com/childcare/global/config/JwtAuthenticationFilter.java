package com.childcare.global.config;

import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.exception.AuthException;
import com.childcare.global.util.InviteCodeGenerator;
import com.childcare.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Supabase JWT 인증 필터
 * - JWT 검증 후 userId로 인증 처리
 * - 첫 로그인 시에만 member 자동 생성
 * - /admin/** 요청 시에만 DB에서 role 확인
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final UUID DEFAULT_DEV_BYPASS_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_DEV_BYPASS_EMAIL = "dev-e2e@local.test";
    private static final String DEFAULT_DEV_BYPASS_NAME = "개발 테스트 사용자";
    private static final String DEFAULT_DEV_BYPASS_REGION_NAME = "서울시 강남구 역삼동";
    private static final String DEFAULT_DEV_BYPASS_REGION_CODE = "1168010100";
    private static final String DEFAULT_DEV_BYPASS_POSTCODE = "06236";

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    @Value("${auth.dev-bypass-token:}")
    private String devBypassToken;
    @Value("${auth.dev-bypass-user-id:}")
    private String devBypassUserId;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/test/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);
            if (isDevBypassToken(token)) {
                authenticateWithDevBypassToken();
            } else if (token != null && jwtUtil.validateToken(token)) {
                UUID userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);
                String path = request.getRequestURI();

                // 첫 로그인: member 없으면 생성
                if (!memberRepository.existsById(userId)) {
                    createMemberFromToken(token, userId, email);
                }

                // /admin/** 요청 시에만 DB에서 role 확인, 그 외는 USER
                String role = Role.USER.name();
                if (path.startsWith("/api/admin/")) {
                    role = memberRepository.findById(userId)
                            .map(m -> m.getRole() != null ? m.getRole().name() : Role.USER.name())
                            .orElse(Role.USER.name());
                }

                List<SimpleGrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(email);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (token != null) {
                sendAuthError(response, "AUTH_009", "유효하지 않은 토큰입니다.");
                return;
            } else {
                sendAuthError(response, "인증이 필요합니다.");
                return;
            }
        } catch (AuthException ex) {
            logger.info("JWT authentication failed: " + ex.getMessage());
            sendAuthError(response, ex.getCode(), ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDevBypassToken(String token) {
        return StringUtils.hasText(devBypassToken) && devBypassToken.equals(token);
    }

    private void authenticateWithDevBypassToken() {
        UUID userId = resolveDevBypassUserId();
        ensureDevBypassMember(userId);
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(DEFAULT_DEV_BYPASS_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UUID resolveDevBypassUserId() {
        if (!StringUtils.hasText(devBypassUserId)) {
            return DEFAULT_DEV_BYPASS_USER_ID;
        }
        try {
            return UUID.fromString(devBypassUserId.trim());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid auth.dev-bypass-user-id, fallback to default: " + devBypassUserId, e);
            return DEFAULT_DEV_BYPASS_USER_ID;
        }
    }

    private void ensureDevBypassMember(UUID userId) {
        Member member = memberRepository.findById(userId)
                .orElseGet(() -> memberRepository.saveAndFlush(Member.builder()
                        .id(userId)
                        .name(DEFAULT_DEV_BYPASS_NAME)
                        .email(DEFAULT_DEV_BYPASS_EMAIL)
                        .regionName(DEFAULT_DEV_BYPASS_REGION_NAME)
                        .regionCode(DEFAULT_DEV_BYPASS_REGION_CODE)
                        .postcode(DEFAULT_DEV_BYPASS_POSTCODE)
                        .role(Role.USER)
                        .inviteCode(inviteCodeGenerator.generate())
                        .provider("DEV")
                        .build()));

        boolean needsUpdate = false;
        if (!StringUtils.hasText(member.getRegionName())) {
            member.setRegionName(DEFAULT_DEV_BYPASS_REGION_NAME);
            needsUpdate = true;
        }
        if (!StringUtils.hasText(member.getRegionCode())) {
            member.setRegionCode(DEFAULT_DEV_BYPASS_REGION_CODE);
            needsUpdate = true;
        }
        if (!StringUtils.hasText(member.getPostcode())) {
            member.setPostcode(DEFAULT_DEV_BYPASS_POSTCODE);
            needsUpdate = true;
        }
        if (member.getRole() == null) {
            member.setRole(Role.USER);
            needsUpdate = true;
        }

        if (needsUpdate) {
            memberRepository.saveAndFlush(member);
        }
    }

    /**
     * 첫 로그인 시 member 자동 생성
     */
    private void createMemberFromToken(String token, UUID userId, String email) {
        Map<String, Object> userMetadata = jwtUtil.getUserMetadataFromToken(token);
        Map<String, Object> appMetadata = jwtUtil.getAppMetadataFromToken(token);

        String name = extractName(userMetadata);
        String profileImageUrl = extractProfileImageUrl(userMetadata);
        String provider = extractProvider(appMetadata);

        Member newMember = Member.builder()
                .id(userId)
                .email(email)
                .name(name != null ? name : "사용자")
                .profileImageUrl(profileImageUrl)
                .provider(provider)
                .inviteCode(inviteCodeGenerator.generate())
                .role(Role.USER)
                .build();

        memberRepository.saveAndFlush(newMember);
        logger.info("Auto-created member: " + userId + " (" + email + ")");
    }

    private String extractName(Map<String, Object> userMetadata) {
        if (userMetadata == null) return null;
        if (userMetadata.get("full_name") != null) return userMetadata.get("full_name").toString();
        if (userMetadata.get("name") != null) return userMetadata.get("name").toString();
        return null;
    }

    private String extractProfileImageUrl(Map<String, Object> userMetadata) {
        if (userMetadata == null) return null;
        if (userMetadata.get("avatar_url") != null) return userMetadata.get("avatar_url").toString();
        if (userMetadata.get("picture") != null) return userMetadata.get("picture").toString();
        return null;
    }

    private String extractProvider(Map<String, Object> appMetadata) {
        if (appMetadata == null) return null;
        if (appMetadata.get("provider") != null) return appMetadata.get("provider").toString().toUpperCase();
        return null;
    }

    private void sendAuthError(HttpServletResponse response, String message) throws IOException {
        sendAuthError(response, "AUTH_001", message);
    }

    private void sendAuthError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"error\",\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"data\":null}");
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
