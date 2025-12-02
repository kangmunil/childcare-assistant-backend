package com.childcare.domain.diary.service;

import com.childcare.domain.diary.dto.DiaryItemResponse;
import com.childcare.domain.diary.entity.CcDiaryItem;
import com.childcare.domain.diary.repository.CcDiaryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryItemService {

    private final CcDiaryItemRepository ccDiaryItemRepository;

    public DiaryItemResponse getAllItems() {
        log.info("Fetching all diary items");

        List<CcDiaryItem> items = ccDiaryItemRepository.findAllActive();

        List<DiaryItemResponse.DiaryItemDto> itemDtos = items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return DiaryItemResponse.builder()
                .status("success")
                .message("일지 항목 조회 성공")
                .data(itemDtos)
                .build();
    }

    public DiaryItemResponse getItemsByDivision(String division) {
        log.info("Fetching diary items by division: {}", division);

        List<CcDiaryItem> items = ccDiaryItemRepository.findByDivision(division);

        List<DiaryItemResponse.DiaryItemDto> itemDtos = items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return DiaryItemResponse.builder()
                .status("success")
                .message("일지 항목 조회 성공")
                .data(itemDtos)
                .build();
    }

    private DiaryItemResponse.DiaryItemDto toDto(CcDiaryItem item) {
        return DiaryItemResponse.DiaryItemDto.builder()
                .id(item.getCcDiSeq())
                .division(item.getCcDiDiv())
                .code(item.getCcDiCode())
                .name(item.getCcDiName())
                .unit(item.getCcDiUnit())
                .build();
    }
}
