package com.childcare.domain.checklist.service;

import com.childcare.domain.checklist.dto.ChecklistDto;
import com.childcare.domain.checklist.mapper.ChecklistMapper;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChecklistService {

    private final ChecklistMapper checklistMapper;
    private final ChildAccessValidator childAccessValidator;

    /**
     * 체크된 항목 조회
     */
    public ApiResponse<List<ChecklistDto>> getCheckedChecklists(Long memberSeq, Long childId) {
        log.info("Fetching checked checklists for child: {}", childId);
        childAccessValidator.validateReadAccess(memberSeq, childId);

        List<ChecklistDto> checklists = checklistMapper.findCheckedByChildId(childId);

        return ApiResponse.success("완료 체크리스트 조회 성공", checklists);
    }

    /**
     * 미체크된 항목 조회
     */
    public ApiResponse<List<ChecklistDto>> getUncheckedChecklists(Long memberSeq, Long childId) {
        log.info("Fetching unchecked checklists for child: {}", childId);
        childAccessValidator.validateReadAccess(memberSeq, childId);

        List<ChecklistDto> checklists = checklistMapper.findUncheckedByChildId(childId);

        return ApiResponse.success("미완료 체크리스트 조회 성공", checklists);
    }
}
