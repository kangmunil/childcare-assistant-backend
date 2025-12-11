package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardItemRepository extends JpaRepository<BoardItem, Long> {

    // 게시판별 게시글 목록 (삭제되지 않은 것만)
    Page<BoardItem> findByBoSeqAndDeleteYnIsNullOrDeleteYnNot(Long boSeq, String deleteYn, Pageable pageable);

    // 게시판별 게시글 목록 (간단 버전)
    List<BoardItem> findByBoSeqAndDeleteYnIsNullOrderByRegDateDesc(Long boSeq);

    // 동네 게시판용: 우편번호 앞3자리로 필터링
    Page<BoardItem> findByBoSeqAndRegUserPostcodeAndDeleteYnIsNull(Long boSeq, Integer regUserPostcode, Pageable pageable);

    // 게시글 단건 조회
    Optional<BoardItem> findByBiSeqAndDeleteYnIsNull(Long biSeq);

    // 고정글 조회 (삭제되지 않은 것만)
    List<BoardItem> findByBoSeqAndFixYnAndDeleteYnIsNullOrderByRegDateDesc(Long boSeq, String fixYn);

    // 인기글 조회 (고정글 제외, 조회수+공감수 상위 3건)
    @Query("SELECT i FROM BoardItem i WHERE i.boSeq = :boSeq AND i.fixYn IS NULL AND (i.deleteYn IS NULL OR i.deleteYn != 'Y') ORDER BY (COALESCE(i.readCount, 0) + COALESCE(i.likeCount, 0)) DESC")
    List<BoardItem> findTop3ByBoSeqAndFixYnIsNullAndDeleteYnIsNullOrderByPopularity(@Param("boSeq") Long boSeq, Pageable pageable);

    // 인기글 조회 (메서드 오버로드 - 기본 3건)
    default List<BoardItem> findTop3ByBoSeqAndFixYnIsNullAndDeleteYnIsNullOrderByPopularity(Long boSeq) {
        return findTop3ByBoSeqAndFixYnIsNullAndDeleteYnIsNullOrderByPopularity(boSeq, Pageable.ofSize(3));
    }

    // 일반글 조회 (고정글 제외, 페이징)
    Page<BoardItem> findByBoSeqAndFixYnIsNullAndDeleteYnIsNullOrderByRegDateDesc(Long boSeq, Pageable pageable);

    // 동네 게시판 일반글 조회 (고정글 제외, 페이징)
    Page<BoardItem> findByBoSeqAndRegUserPostcodeAndFixYnIsNullAndDeleteYnIsNullOrderByRegDateDesc(Long boSeq, Integer regUserPostcode, Pageable pageable);
}
