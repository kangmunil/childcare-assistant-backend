package com.childcare.domain.member.repository;

import com.childcare.domain.member.entity.LeaveMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeaveMemberRepository extends JpaRepository<LeaveMember, UUID> {
}
