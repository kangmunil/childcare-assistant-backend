package com.childcare.domain.auth.repository;

import com.childcare.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByMbIdAndRevokedAtIsNull(UUID mbId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.mbId = :mbId AND rt.revokedAt IS NULL")
    int revokeAllByMbId(@Param("mbId") UUID mbId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
