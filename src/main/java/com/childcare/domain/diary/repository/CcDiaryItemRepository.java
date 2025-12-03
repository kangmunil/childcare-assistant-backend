package com.childcare.domain.diary.repository;

import com.childcare.domain.diary.entity.CcDiaryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CcDiaryItemRepository extends JpaRepository<CcDiaryItem, Long> {

}
