package com.childcare.domain.child.repository;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.parent.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {

    @Query("SELECT c FROM Child c " +
           "JOIN Parent p ON c.chSeq = p.chSeq " +
           "JOIN Member m ON p.mbSeq = m.mbSeq " +
           "WHERE p.mbSeq = :memberSeq " +
           "AND m.leaveDate IS NULL " +
           "AND (c.deleteYn IS NULL OR c.deleteYn <> 'Y')")
    List<Child> findActiveChildrenByMemberSeq(@Param("memberSeq") Long memberSeq);
}
