package com.childcare.domain.auth.controller;

import com.childcare.domain.auth.dto.AuthRequest;
import com.childcare.domain.auth.dto.AuthResponse;
import com.childcare.domain.auth.dto.LoginRequest;
import com.childcare.domain.auth.dto.RegisterRequest;
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
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
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
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("General login request received for user: {}", request.getId());
        AuthResponse response = authService.login(request.getId(), request.getPassword());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for user: {}", request.getId());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
}