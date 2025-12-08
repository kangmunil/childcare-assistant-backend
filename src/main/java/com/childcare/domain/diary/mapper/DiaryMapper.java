package com.childcare.domain.diary.mapper;

import com.childcare.domain.diary.dto.DiaryStatDto;
import com.childcare.domain.diary.dto.DiarySummaryDto;
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

    Optional<ChildDiary> findActiveDiaryById(@Param("childId") Long childId, @Param("diSeq") Long diSeq);

    // 일지 요약 (항목별 합계)
    List<DiarySummaryDto.ItemSummary> findDiarySummaryByChildId(@Param("childId") Long childId, @Param("diDate") String diDate);

    // 일지 요약 (division 기준 모든 항목, 기록 없으면 0)
    List<DiarySummaryDto.ItemSummary> findDailySummaryByDivision(@Param("childId") Long childId, @Param("diDate") String diDate, @Param("division") String division);

    // CcDiaryItem 관련 쿼리
    List<CcDiaryItem> findAllActiveItems();

    List<CcDiaryItem> findActiveItemsByDivision(@Param("division") String division);

    // 기간별 일지 통계
    List<DiaryStatDto.DiaryStat> findDiaryStatsByPeriod(
            @Param("childId") Long childId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
