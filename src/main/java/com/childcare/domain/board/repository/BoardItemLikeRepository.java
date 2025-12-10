package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardItemLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardItemLikeRepository extends JpaRepository<BoardItemLike, Long> {

    // 특정 사용자가 특정 게시글에 공감했는지 확인
    Optional<BoardItemLike> findByBiSeqAndMbSeq(Long biSeq, Long mbSeq);

    // 특정 사용자가 특정 게시글에 공감했는지 여부
    boolean existsByBiSeqAndMbSeq(Long biSeq, Long mbSeq);

    // 특정 게시글의 공감 수
    long countByBiSeq(Long biSeq);
}
