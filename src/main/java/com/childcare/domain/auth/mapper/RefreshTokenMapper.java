package com.childcare.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
public interface RefreshTokenMapper {

    /**
     * 특정 회원의 모든 유효한 Refresh Token 폐기
     */
    int revokeAllByMbId(@Param("mbId") UUID mbId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 만료된 Refresh Token 삭제
     */
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}