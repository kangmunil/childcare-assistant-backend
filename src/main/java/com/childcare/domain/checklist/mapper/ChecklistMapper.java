package com.childcare.domain.checklist.mapper;

import com.childcare.domain.checklist.dto.ChecklistDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChecklistMapper {

    // 체크된 항목 조회
    List<ChecklistDto> findCheckedByChildId(@Param("childId") Long childId);

    // 미체크된 항목 조회
    List<ChecklistDto> findUncheckedByChildId(@Param("childId") Long childId);
}
