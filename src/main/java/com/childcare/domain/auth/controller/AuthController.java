package com.childcare.domain.auth.controller;

import com.childcare.domain.auth.dto.AuthRequest;
import com.childcare.domain.auth.dto.AuthResponse;
import com.childcare.domain.auth.dto.RefreshRequest;
import com.childcare.domain.auth.service.AuthService;
import com.childcare.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 OAuth 로그인
     */
    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> kakaoAuth(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("Kakao authentication request received");
            AuthResponse response = authService.authenticateKakao(request.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kakao authentication error", e);
            AuthResponse errorResponse = AuthResponse.builder()
                    .status("error")
                    .message("카카오 로그인 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 구글 OAuth 로그인
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("Google authentication request received");
            AuthResponse response = authService.authenticateGoogle(request.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Google authentication error", e);
            AuthResponse errorResponse = AuthResponse.builder()
                    .status("error")
                    .message("구글 로그인 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 테스트용 회원가입/로그인 (OAuth API 없이 테스트용)
     * TODO:소셜 로그인 연동 후 삭제해야함
     */
    @PostMapping("/test/register")
    public ResponseEntity<AuthResponse> testRegister(
            @RequestParam String email,
            @RequestParam(defaultValue = "테스트유저") String name) {
        log.info("Test registration request for email: {}", email);
        AuthResponse response = authService.testRegister(email, name);
        return ResponseEntity.ok(response);
    }

    /**
     * Access Token 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            log.info("Token refresh request received");
            AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            AuthResponse errorResponse = AuthResponse.builder()
                    .status("error")
                    .message("토큰 갱신 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 로그아웃 (인증 필요)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getPrincipal();
            log.info("Logout request for user: {}", userId);
            authService.logout(userId);
            return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("AUTH_010", "로그아웃 실패: " + e.getMessage()));
        }
    }

    /**
     * Refresh Token으로 로그아웃 (인증 불필요)
     */
    @PostMapping("/logout/token")
    public ResponseEntity<ApiResponse<Void>> logoutByToken(@Valid @RequestBody RefreshRequest request) {
        try {
            log.info("Logout by refresh token request received");
            authService.logoutByRefreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("AUTH_010", "로그아웃 실패: " + e.getMessage()));
        }
    }
}
