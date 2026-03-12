package com.childcare.domain.member.repository;

import com.childcare.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);

    Optional<Member> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
