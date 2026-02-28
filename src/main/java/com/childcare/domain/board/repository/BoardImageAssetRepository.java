package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardImageAssetRepository extends JpaRepository<BoardImageAsset, Long> {
    Optional<BoardImageAsset> findByBfSeq(Long bfSeq);
    List<BoardImageAsset> findByBfSeqIn(Collection<Long> bfSeqList);
}
