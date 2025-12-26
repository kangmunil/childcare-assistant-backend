package com.childcare.domain.checklist.service;

import com.childcare.domain.checklist.dto.ChecklistDto;
import com.childcare.domain.checklist.mapper.ChecklistMapper;
import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChecklistException;
import com.childcare.global.exception.ChecklistException.ChecklistErrorCode;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChecklistService {

    private final ChecklistMapper checklistMapper;
    private final ChildMapper childMapper;
    private final ChildAccessValidator childAccessValidator;

    /**
     * 체크된 항목 조회
     */
    public ApiResponse<List<ChecklistDto>> getCheckedChecklists(UUID memberId, Long childId, String div) {
        log.info("Fetching checked checklists for child: {}", childId);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<ChecklistDto> checklists = checklistMapper.findCheckedByChildId(childId, div);

        return ApiResponse.success("완료 체크리스트 조회 성공", checklists);
    }

    /**
     * 미체크된 항목 조회
     */
    public ApiResponse<List<ChecklistDto>> getUncheckedChecklists(UUID memberId, Long childId, String div) {
        log.info("Fetching unchecked checklists for child: {}", childId);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<ChecklistDto> checklists = checklistMapper.findUncheckedByChildId(childId, div);

        return ApiResponse.success("미완료 체크리스트 조회 성공", checklists);
    }

    /**
     * 체크 추가
     */
    @Transactional
    public ApiResponse<Void> addCheck(UUID memberId, Long childId, Long itemId) {
        log.info("Adding check for child: {}, item: {}", childId, itemId);

        // 자녀 존재 여부 확인
        Child child = childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        childAccessValidator.validateWriteAccess(memberId, childId);

        // 항목 존재 여부 확인
        if (!checklistMapper.existsChecklistItem(itemId)) {
            throw new ChecklistException(ChecklistErrorCode.ITEM_NOT_FOUND);
        }

        // 이미 체크되어 있는지 확인
        if (checklistMapper.isChecked(childId, itemId)) {
            throw new ChecklistException(ChecklistErrorCode.ALREADY_CHECKED);
        }

        String gcDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        checklistMapper.insertCheck(childId, itemId, gcDate, memberId);

        return ApiResponse.success("체크리스트 체크 완료", null);
    }

    /**
     * 체크 삭제
     */
    @Transactional
    public ApiResponse<Void> removeCheck(UUID memberId, Long childId, Long itemId) {
        log.info("Removing check for child: {}, item: {}", childId, itemId);

        // 자녀 존재 여부 확인
        Child child = childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        childAccessValidator.validateWriteAccess(memberId, childId);

        // 항목 존재 여부 확인
        if (!checklistMapper.existsChecklistItem(itemId)) {
            throw new ChecklistException(ChecklistErrorCode.ITEM_NOT_FOUND);
        }

        // 체크되어 있는지 확인
        if (!checklistMapper.isChecked(childId, itemId)) {
            throw new ChecklistException(ChecklistErrorCode.NOT_CHECKED);
        }

        checklistMapper.deleteCheck(childId, itemId, memberId);

        return ApiResponse.success("체크리스트 체크 해제 완료", null);
    }
}
