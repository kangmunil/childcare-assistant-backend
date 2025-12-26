package com.childcare.domain.auth.controller;

import com.childcare.domain.auth.dto.AuthRequest;
import com.childcare.domain.auth.dto.AuthResponse;
import com.childcare.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     */
    @PostMapping("/test/register")
    public ResponseEntity<AuthResponse> testRegister(
            @RequestParam String email,
            @RequestParam(defaultValue = "테스트유저") String name) {
        log.info("Test registration request for email: {}", email);
        AuthResponse response = authService.testRegister(email, name);
        return ResponseEntity.ok(response);
    }
}
