package com.childcare.domain.diary.repository;

import com.childcare.domain.diary.entity.ChildDiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildDiaryRepository extends JpaRepository<ChildDiary, Long> {

    @Query("SELECT d FROM ChildDiary d WHERE d.chSeq = :chSeq AND (d.deleteYn IS NULL OR d.deleteYn <> 'Y') ORDER BY d.diDate DESC, d.diTime DESC")
    List<ChildDiary> findByChildSeq(@Param("chSeq") Long chSeq);

    @Query("SELECT d FROM ChildDiary d WHERE d.chSeq = :chSeq AND d.diDate = :diDate AND (d.deleteYn IS NULL OR d.deleteYn <> 'Y') ORDER BY d.diTime DESC")
    List<ChildDiary> findByChildSeqAndDate(@Param("chSeq") Long chSeq, @Param("diDate") String diDate);

    @Query("SELECT d FROM ChildDiary d WHERE d.diSeq = :diSeq AND (d.deleteYn IS NULL OR d.deleteYn <> 'Y')")
    Optional<ChildDiary> findActiveById(@Param("diSeq") Long diSeq);
}
