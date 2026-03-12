package com.childcare.domain.checklist.mapper;

import com.childcare.domain.checklist.dto.ChecklistDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ChecklistMapper {

    // 체크된 항목 조회
    List<ChecklistDto> findCheckedByChildId(@Param("childId") Long childId, @Param("itemDiv") String div);

    // 미체크된 항목 조회
    List<ChecklistDto> findUncheckedByChildId(@Param("childId") Long childId, @Param("itemDiv") String div);

    // 체크리스트 항목 존재 여부 확인
    boolean existsChecklistItem(@Param("itemId") Long itemId);

    // 체크 여부 확인
    int isChecked(@Param("childId") Long childId, @Param("itemId") Long itemId);

    // 체크 추가
    void insertCheck(@Param("childId") Long childId, @Param("itemId") Long itemId,
                     @Param("gcDate") String gcDate, @Param("regId") UUID regId);

    // 체크 삭제 (soft delete)
    void deleteCheck(@Param("childId") Long childId, @Param("itemId") Long itemId,
                     @Param("deleteId") UUID deleteId);
}
