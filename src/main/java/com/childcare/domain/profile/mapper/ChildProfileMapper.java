package com.childcare.domain.profile.mapper;

import com.childcare.domain.profile.dto.ChildProfileRecord;
import com.childcare.domain.profile.dto.ChildProfileSummaryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ChildProfileMapper {

    Optional<ChildProfileRecord> findProfileByChildId(@Param("childId") Long childId);

    Optional<ChildProfileSummaryRecord> findSummaryByChildId(@Param("childId") Long childId);

    void insertDefaultProfileIfAbsent(@Param("childId") Long childId, @Param("updatedBy") UUID updatedBy);

    void insertSummaryIfAbsent(@Param("childId") Long childId,
                               @Param("summaryText") String summaryText,
                               @Param("summaryVersion") Integer summaryVersion);

    void upsertProfile(@Param("childId") Long childId,
                       @Param("health") String health,
                       @Param("routine") String routine,
                       @Param("development") String development,
                       @Param("education") String education,
                       @Param("safety") String safety,
                       @Param("misc") String misc,
                       @Param("updatedBy") UUID updatedBy);

    void upsertSummary(@Param("childId") Long childId,
                       @Param("summaryText") String summaryText,
                       @Param("summaryVersion") Integer summaryVersion);
}
