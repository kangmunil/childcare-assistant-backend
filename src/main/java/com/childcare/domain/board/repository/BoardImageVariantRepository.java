package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardImageVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BoardImageVariantRepository extends JpaRepository<BoardImageVariant, Long> {
    List<BoardImageVariant> findByBiaSeq(Long biaSeq);
    List<BoardImageVariant> findByBiaSeqIn(Collection<Long> biaSeqList);
}
