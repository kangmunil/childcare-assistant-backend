package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardCommentLikeRepository extends JpaRepository<BoardCommentLike, Long> {

    // 특정 사용자가 특정 댓글에 공감했는지 확인
    Optional<BoardCommentLike> findByBcSeqAndMbSeq(Long bcSeq, Long mbSeq);

    // 특정 사용자가 특정 댓글에 공감했는지 여부
    boolean existsByBcSeqAndMbSeq(Long bcSeq, Long mbSeq);

    // 특정 댓글의 공감 수
    long countByBcSeq(Long bcSeq);
}
