package com.childcare.domain.board.repository;

import com.childcare.domain.board.entity.BoardImageJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardImageJobRepository extends JpaRepository<BoardImageJob, Long> {
}
