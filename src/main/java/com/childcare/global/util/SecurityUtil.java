package com.childcare.global.util;

import com.childcare.global.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 현재 로그인한 회원의 memberSeq를 반환합니다.
     * 인증되지 않은 경우 UnauthorizedException을 발생시킵니다.
     */
    public static Long getCurrentMemberSeq() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long) {
            return (Long) principal;
        }

        if ("anonymousUser".equals(principal)) {
            throw new UnauthorizedException();
        }

        throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
    }
}
