package com.childcare.domain.diary.repository;

import com.childcare.domain.diary.entity.ChildDiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildDiaryRepository extends JpaRepository<ChildDiary, Long> {

}
