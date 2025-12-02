package com.childcare.domain.diary.service;

import com.childcare.domain.diary.dto.ChildDiaryRequest;
import com.childcare.domain.diary.dto.ChildDiaryResponse;
import com.childcare.domain.diary.entity.CcDiaryItem;
import com.childcare.domain.diary.entity.ChildDiary;
import com.childcare.domain.diary.repository.CcDiaryItemRepository;
import com.childcare.domain.diary.repository.ChildDiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildDiaryService {

    private final ChildDiaryRepository childDiaryRepository;
    private final CcDiaryItemRepository ccDiaryItemRepository;

    public ChildDiaryResponse getDiariesByChild(Long childId) {
        log.info("Fetching diaries for child: {}", childId);

        List<ChildDiary> diaries = childDiaryRepository.findByChildSeq(childId);
        Map<Long, CcDiaryItem> itemMap = getItemMap();

        List<ChildDiaryResponse.ChildDiaryDto> diaryDtos = diaries.stream()
                .map(diary -> toDto(diary, itemMap.get(diary.getCcDiSeq())))
                .collect(Collectors.toList());

        return ChildDiaryResponse.builder()
                .status("success")
                .message("성장일지 조회 성공")
                .data(diaryDtos)
                .build();
    }

    public ChildDiaryResponse getDiariesByChildAndDate(Long childId, String date) {
        log.info("Fetching diaries for child: {} on date: {}", childId, date);

        List<ChildDiary> diaries = childDiaryRepository.findByChildSeqAndDate(childId, date);
        Map<Long, CcDiaryItem> itemMap = getItemMap();

        List<ChildDiaryResponse.ChildDiaryDto> diaryDtos = diaries.stream()
                .map(diary -> toDto(diary, itemMap.get(diary.getCcDiSeq())))
                .collect(Collectors.toList());

        return ChildDiaryResponse.builder()
                .status("success")
                .message("성장일지 조회 성공")
                .data(diaryDtos)
                .build();
    }

    @Transactional
    public ChildDiaryResponse createDiary(Long memberSeq, Long childId, ChildDiaryRequest request) {
        log.info("Creating diary for child: {}", childId);

        // 필수값 검증
        if (request.getItemId() == null) {
            throw new IllegalArgumentException("항목은 필수 입력값입니다.");
        }
        if (request.getDiDate() == null || request.getDiDate().isBlank()) {
            throw new IllegalArgumentException("날짜는 필수 입력값입니다.");
        }
        if (request.getDiTime() == null || request.getDiTime().isBlank()) {
            throw new IllegalArgumentException("시간은 필수 입력값입니다.");
        }

        // 항목 존재 여부 확인
        CcDiaryItem item = ccDiaryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 항목입니다."));

        ChildDiary diary = ChildDiary.builder()
                .chSeq(childId)
                .ccDiSeq(request.getItemId())
                .diDate(request.getDiDate())
                .diTime(request.getDiTime())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .regUserSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .deleteYn("N")
                .build();

        ChildDiary savedDiary = childDiaryRepository.save(diary);

        return ChildDiaryResponse.builder()
                .status("success")
                .message("성장일지 등록 성공")
                .data(List.of(toDto(savedDiary, item)))
                .build();
    }

    @Transactional
    public ChildDiaryResponse updateDiary(Long memberSeq, Long childId, Long diaryId, ChildDiaryRequest request) {
        log.info("Updating diary {} for child: {}", diaryId, childId);

        ChildDiary diary = childDiaryRepository.findActiveById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("성장일지를 찾을 수 없습니다."));

        if (!diary.getChSeq().equals(childId)) {
            throw new IllegalArgumentException("해당 자녀의 성장일지가 아닙니다.");
        }

        // 항목 변경 시 존재 여부 확인
        CcDiaryItem item;
        if (request.getItemId() != null) {
            item = ccDiaryItemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 항목입니다."));
            diary.setCcDiSeq(request.getItemId());
        } else {
            item = ccDiaryItemRepository.findById(diary.getCcDiSeq())
                    .orElse(null);
        }

        if (request.getDiDate() != null && !request.getDiDate().isBlank()) {
            diary.setDiDate(request.getDiDate());
        }
        if (request.getDiTime() != null && !request.getDiTime().isBlank()) {
            diary.setDiTime(request.getDiTime());
        }
        diary.setAmount(request.getAmount());
        diary.setMemo(request.getMemo());

        ChildDiary updatedDiary = childDiaryRepository.save(diary);

        return ChildDiaryResponse.builder()
                .status("success")
                .message("성장일지 수정 성공")
                .data(List.of(toDto(updatedDiary, item)))
                .build();
    }

    @Transactional
    public ChildDiaryResponse deleteDiary(Long memberSeq, Long childId, Long diaryId) {
        log.info("Deleting diary {} for child: {}", diaryId, childId);

        ChildDiary diary = childDiaryRepository.findActiveById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("성장일지를 찾을 수 없습니다."));

        if (!diary.getChSeq().equals(childId)) {
            throw new IllegalArgumentException("해당 자녀의 성장일지가 아닙니다.");
        }

        diary.setDeleteYn("Y");
        diary.setDeleteUserSeq(String.valueOf(memberSeq));
        diary.setDeleteDate(LocalDateTime.now());

        childDiaryRepository.save(diary);

        return ChildDiaryResponse.builder()
                .status("success")
                .message("성장일지 삭제 성공")
                .data(null)
                .build();
    }

    private Map<Long, CcDiaryItem> getItemMap() {
        return ccDiaryItemRepository.findAllActive().stream()
                .collect(Collectors.toMap(CcDiaryItem::getCcDiSeq, Function.identity()));
    }

    private ChildDiaryResponse.ChildDiaryDto toDto(ChildDiary diary, CcDiaryItem item) {
        return ChildDiaryResponse.ChildDiaryDto.builder()
                .id(diary.getDiSeq())
                .childId(diary.getChSeq())
                .itemId(diary.getCcDiSeq())
                .itemDivision(item != null ? item.getCcDiDiv() : null)
                .itemCode(item != null ? item.getCcDiCode() : null)
                .itemName(item != null ? item.getCcDiName() : null)
                .diDate(diary.getDiDate())
                .diTime(diary.getDiTime())
                .amount(diary.getAmount())
                .memo(diary.getMemo())
                .build();
    }
}
