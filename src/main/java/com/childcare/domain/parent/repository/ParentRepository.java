package com.childcare.domain.parent.repository;

import com.childcare.domain.parent.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByMbIdAndChSeq(UUID mbId, Long chSeq);

    List<Parent> findByMbId(UUID mbId);

    List<Parent> findByChSeq(Long chSeq);

    void deleteByMbIdAndChSeq(UUID mbId, Long chSeq);
}
