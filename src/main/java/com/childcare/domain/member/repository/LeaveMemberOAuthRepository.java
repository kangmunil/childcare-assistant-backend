package com.childcare.domain.member.repository;

import com.childcare.domain.member.entity.LeaveMemberOAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveMemberOAuthRepository extends JpaRepository<LeaveMemberOAuth, Long> {
}
