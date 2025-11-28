package com.childcare.domain.member.repository;

import com.childcare.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    Optional<Member> findByEmail(String email);
    
    Optional<Member> findByIdAndPassword(String id, String password);
    
    Optional<Member> findById(String id);
    
    boolean existsByEmail(String email);
    
    boolean existsById(String id);
}