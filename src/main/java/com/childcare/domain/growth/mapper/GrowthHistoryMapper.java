package com.childcare.domain.growth.mapper;

import com.childcare.domain.growth.entity.ChildGrowHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface GrowthHistoryMapper {

    Optional<ChildGrowHistory> findActiveHistoryById(@Param("childId") Long childId, @Param("historyId") Long historyId);

    void insertHistory(ChildGrowHistory history);

    int updateHistory(@Param("childId") Long childId, @Param("historyId") Long historyId,
                      @Param("height") String height, @Param("weight") String weight);

    int softDeleteHistory(@Param("childId") Long childId, @Param("historyId") Long historyId,
                          @Param("deleteId") java.util.UUID deleteId);
}
