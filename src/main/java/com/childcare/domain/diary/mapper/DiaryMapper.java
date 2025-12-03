package com.childcare.domain.diary.mapper;

import com.childcare.domain.diary.entity.CcDiaryItem;
import com.childcare.domain.diary.entity.ChildDiary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DiaryMapper {

    // ChildDiary 관련 쿼리
    List<ChildDiary> findDiariesByChildId(@Param("childId") Long childId);

    List<ChildDiary> findDiariesByChildIdAndDate(@Param("childId") Long childId, @Param("diDate") String diDate);

    Optional<ChildDiary> findActiveDiaryById(@Param("diSeq") Long diSeq);

    // CcDiaryItem 관련 쿼리
    List<CcDiaryItem> findAllActiveItems();

    List<CcDiaryItem> findActiveItemsByDivision(@Param("division") String division);
}
