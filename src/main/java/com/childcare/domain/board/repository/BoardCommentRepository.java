package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    /**
     * 댓글 목록 조회 (정렬: 고정댓글 우선 > 부모-자식 그룹핑 > 등록일순)
     * - depth 1까지만 지원 (댓글 + 대댓글)
     * - 고정 댓글(fix_yn='Y')이 최상단
     * - 같은 부모의 댓글끼리 그룹핑
     * - 부모 댓글이 먼저, 대댓글은 등록일순
     */
    // 게시글별 댓글 수
    long countByBiSeqAndDeleteYnIsNull(Long biSeq);
}
