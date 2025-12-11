package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardItemRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardItemReadRepository extends JpaRepository<BoardItemRead, Long> {

    // 특정 사용자가 특정 게시글을 조회했는지 확인
    Optional<BoardItemRead> findByBiSeqAndMbSeq(Long biSeq, Long mbSeq);

    // 특정 사용자가 특정 게시글을 조회했는지 여부
    boolean existsByBiSeqAndMbSeq(Long biSeq, Long mbSeq);
}
