package com.childcare.domain.parent.repository;

import com.childcare.domain.parent.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Integer> {

    Optional<Parent> findByMbSeqAndChSeq(Long mbSeq, Long chSeq);

    List<Parent> findByMbSeq(Long mbSeq);

    List<Parent> findByChSeq(Long chSeq);

    void deleteByMbSeqAndChSeq(Long mbSeq, Long chSeq);
}
