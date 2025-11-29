package com.childcare.domain.member.repository;

import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.MemberOAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberOAuthRepository extends JpaRepository<MemberOAuth, Long> {
    
    Optional<MemberOAuth> findByProviderAndProviderId(MemberOAuth.OAuthProvider provider, String providerId);
    
    List<MemberOAuth> findByMember(Member member);
    
    boolean existsByProviderAndProviderId(MemberOAuth.OAuthProvider provider, String providerId);
}