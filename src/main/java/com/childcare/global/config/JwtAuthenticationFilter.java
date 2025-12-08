package com.childcare.global.config;

import com.childcare.global.util.JwtUtil;
import com.childcare.global.util.JwtUtil.TokenExpiredException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    private final List<String> excludedPaths = Arrays.asList("/api/auth/", "/api/test/");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException{
        // 로그인, 회원가입 API URL이 포함되는지 확인하는 함수
        String path = request.getRequestURI();
        boolean shouldNotFilter = excludedPaths.stream().anyMatch(path::startsWith);
        return excludedPaths.stream().anyMatch(path::startsWith);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                // role이 없으면 기본값 USER
                if (role == null || role.isEmpty()) {
                    role = "USER";
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
        } catch(TokenExpiredException ex) {
            logger.info("JWT token is expired: " + ex.getMessage());
            sendAuthError(response, "AUTH_008", "토큰이 만료되었습니다.");
            return;
        } catch(JwtException ex) {
            logger.info("Failed to authorize/authenticate with JWT due to " + ex.getMessage());
            sendAuthError(response, "AUTH_009", "유효하지 않은 토큰입니다.");
            return;
        }

        filterChain.doFilter(request, response);
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