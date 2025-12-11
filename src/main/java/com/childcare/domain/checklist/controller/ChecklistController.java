package com.childcare.domain.checklist.controller;

import com.childcare.domain.checklist.dto.ChecklistDto;
import com.childcare.domain.checklist.service.ChecklistService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/children/{childId}/checklists")
@RequiredArgsConstructor
@Slf4j
public class ChecklistController {

    private final ChecklistService checklistService;

    /**
     * 체크된 항목 조회
     */
    @GetMapping("/checked")
    public ResponseEntity<ApiResponse<List<ChecklistDto>>> getCheckedChecklists(
            @PathVariable Long childId) {
        Long memberSeq = getMemberSeq();
        log.info("Get checked checklists for child: {}", childId);

        ApiResponse<List<ChecklistDto>> response = checklistService.getCheckedChecklists(memberSeq, childId);
        return ResponseEntity.ok(response);
    }

    /**
     * 미체크된 항목 조회
     */
    @GetMapping("/unchecked")
    public ResponseEntity<ApiResponse<List<ChecklistDto>>> getUncheckedChecklists(
            @PathVariable Long childId) {
        Long memberSeq = getMemberSeq();
        log.info("Get unchecked checklists for child: {}", childId);

        ApiResponse<List<ChecklistDto>> response = checklistService.getUncheckedChecklists(memberSeq, childId);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        return SecurityUtil.getCurrentMemberSeq();
    }
}
