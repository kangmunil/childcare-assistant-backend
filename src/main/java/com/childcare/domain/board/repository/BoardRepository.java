package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByBoCode(String boCode);

    Optional<Board> findByBoSlug(String boSlug);

    List<Board> findByBoUseYn(String boUseYn);

    Optional<Board> findByBoSeqAndBoUseYn(Long boSeq, String boUseYn);

    List<Board> findByBoUseYnAndBoReadAuth(String boUseYn, String boReadAuth);
}
