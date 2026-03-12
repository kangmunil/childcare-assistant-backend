package com.childcare.global.util;

import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * Supabase JWT 검증 유틸리티 (ES256 JWKS 방식)
 * - Supabase JWKS 엔드포인트에서 공개키를 가져와 JWT 검증
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${supabase.url}")
    private String supabaseUrl;

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @PostConstruct
    public void init() {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl));
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, keySource);

            jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            log.info("JwtUtil initialized with JWKS URL: {}", jwksUrl);
        } catch (Exception e) {
            log.error("Failed to initialize JwtUtil: {}", e.getMessage());
            throw new RuntimeException("JWT processor initialization failed", e);
        }
    }

    /**
     * Supabase JWT에서 사용자 ID(UUID) 추출
     */
    public UUID getUserIdFromToken(String token) {
        JWTClaimsSet claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Supabase JWT에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        JWTClaimsSet claims = parseClaims(token);
        return (String) claims.getClaim("email");
    }

    /**
     * Supabase JWT에서 user_metadata 추출
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserMetadataFromToken(String token) {
        JWTClaimsSet claims = parseClaims(token);
        return (Map<String, Object>) claims.getClaim("user_metadata");
    }

    /**
     * Supabase JWT에서 app_metadata 추출
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppMetadataFromToken(String token) {
        JWTClaimsSet claims = parseClaims(token);
        return (Map<String, Object>) claims.getClaim("app_metadata");
    }

    /**
     * Supabase JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private JWTClaimsSet parseClaims(String token) {
        try {
            return jwtProcessor.process(token, null);
        } catch (com.nimbusds.jwt.proc.BadJWTException e) {
            if (e.getMessage().contains("expired") || e.getMessage().contains("Expired")) {
                log.error("JWT token is expired: {}", e.getMessage());
                throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
            }
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}