package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {

    // 게시글별 첨부파일 목록
    List<BoardFile> findByBiSeq(Long biSeq);

    // 게시글 목록의 대표 이미지 조회용
    List<BoardFile> findByBiSeqInOrderByRegDateAsc(List<Long> biSeqList);

    // 게시글별 첨부파일 삭제
    void deleteByBiSeq(Long biSeq);
}
