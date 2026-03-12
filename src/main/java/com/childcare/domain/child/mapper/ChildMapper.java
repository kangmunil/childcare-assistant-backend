package com.childcare.domain.child.mapper;

import com.childcare.domain.child.dto.GrowthHistoryDto;
import com.childcare.domain.child.dto.GrowthHistoryStatDto;
import com.childcare.domain.child.entity.Child;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ChildMapper {

    List<Child> findActiveChildrenByMemberId(@Param("memberId") UUID memberId);

    Optional<Child> findActiveChildById(@Param("childId") Long childId);

    List<GrowthHistoryDto> findGrowthHistory(@Param("childId") Long childId);

    List<GrowthHistoryStatDto.GrowthStat> findGrowthHistoryStatsByPeriod(
            @Param("childId") Long childId,
            @Param("periodType") String periodType,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    void clearPrimaryByMemberId(@Param("memberId") UUID memberId);

    void setPrimary(@Param("childId") Long childId);
}
