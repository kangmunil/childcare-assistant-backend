package com.childcare.domain.diary.repository;

import com.childcare.domain.diary.entity.CcDiaryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CcDiaryItemRepository extends JpaRepository<CcDiaryItem, Long> {

    @Query("SELECT c FROM CcDiaryItem c WHERE (c.deleteYn IS NULL OR c.deleteYn <> 'Y')")
    List<CcDiaryItem> findAllActive();

    @Query("SELECT c FROM CcDiaryItem c WHERE c.ccDiDiv = :div AND (c.deleteYn IS NULL OR c.deleteYn <> 'Y')")
    List<CcDiaryItem> findByDivision(@Param("div") String div);
}
