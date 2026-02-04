package com.childcare.domain.diary.service;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.diary.dto.ChildDiaryDto;
import com.childcare.domain.diary.dto.ChildDiaryRequest;
import com.childcare.domain.diary.dto.DiaryMemoDto;
import com.childcare.domain.diary.dto.DiaryMemoRequest;
import com.childcare.domain.diary.dto.DiaryStatDto;
import com.childcare.domain.diary.dto.DiarySummaryDto;
import com.childcare.domain.diary.entity.CcDiaryItem;
import com.childcare.domain.diary.entity.ChildDiary;
import com.childcare.domain.diary.entity.ChildDiaryMemo;
import com.childcare.domain.diary.mapper.DiaryMapper;
import com.childcare.domain.diary.repository.CcDiaryItemRepository;
import com.childcare.domain.diary.repository.ChildDiaryRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import com.childcare.global.exception.DiaryException;
import com.childcare.global.exception.DiaryException.DiaryErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildDiaryService {

    private final ChildDiaryRepository childDiaryRepository;
    private final CcDiaryItemRepository ccDiaryItemRepository;
    private final DiaryMapper diaryMapper;
    private final ChildMapper childMapper;
    private final ChildAccessValidator childAccessValidator;

    public ApiResponse<List<ChildDiaryDto>> getDiariesByChild(UUID memberId, Long childId) {
        log.info("Fetching diaries for child: {}", childId);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<ChildDiary> diaries = diaryMapper.findDiariesByChildId(childId);
        Map<Long, CcDiaryItem> itemMap = getItemMap();

        List<ChildDiaryDto> diaryDtos = diaries.stream()
                .map(diary -> toDto(diary, itemMap.get(diary.getCcDiSeq())))
                .collect(Collectors.toList());

        return ApiResponse.success("성장일지 조회 성공", diaryDtos);
    }

    public ApiResponse<List<ChildDiaryDto>> getDiariesByChildAndDate(UUID memberId, Long childId, String date) {
        log.info("Fetching diaries for child: {} on date: {}", childId, date);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<ChildDiary> diaries = diaryMapper.findDiariesByChildIdAndDate(childId, date);
        Map<Long, CcDiaryItem> itemMap = getItemMap();

        List<ChildDiaryDto> diaryDtos = diaries.stream()
                .map(diary -> toDto(diary, itemMap.get(diary.getCcDiSeq())))
                .collect(Collectors.toList());

        return ApiResponse.success("성장일지 조회 성공", diaryDtos);
    }

    @Transactional
    public ApiResponse<List<ChildDiaryDto>> createDiary(UUID memberId, Long childId, ChildDiaryRequest request) {
        log.info("Creating diary for child: {}", childId);
        childAccessValidator.validateWriteAccess(memberId, childId);

        if (request.getItemId() == null) {
            throw new DiaryException(DiaryErrorCode.ITEM_REQUIRED);
        }
        if (request.getDiDate() == null || request.getDiDate().isBlank()) {
            throw new DiaryException(DiaryErrorCode.DATE_REQUIRED);
        }
        if (request.getDiTime() == null || request.getDiTime().isBlank()) {
            throw new DiaryException(DiaryErrorCode.TIME_REQUIRED);
        }

        CcDiaryItem item = ccDiaryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.ITEM_NOT_FOUND));

        ChildDiary diary = ChildDiary.builder()
                .chSeq(childId)
                .ccDiSeq(request.getItemId())
                .diDate(request.getDiDate())
                .diTime(request.getDiTime())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .deleteYn("N")
                .build();

        ChildDiary savedDiary = childDiaryRepository.save(diary);

        return ApiResponse.success("성장일지 등록 성공", List.of(toDto(savedDiary, item)));
    }

    @Transactional
    public ApiResponse<List<ChildDiaryDto>> updateDiary(UUID memberId, Long childId, Long diaryId, ChildDiaryRequest request) {
        log.info("Updating diary {} for child: {}", diaryId, childId);
        childAccessValidator.validateWriteAccess(memberId, childId);

        ChildDiary diary = diaryMapper.findActiveDiaryById(childId, diaryId)
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.NOT_FOUND));

        CcDiaryItem item;
        if (request.getItemId() != null) {
            item = ccDiaryItemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new DiaryException(DiaryErrorCode.ITEM_NOT_FOUND));
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

        return ApiResponse.success("성장일지 수정 성공", List.of(toDto(updatedDiary, item)));
    }

    @Transactional
    public ApiResponse<Void> deleteDiary(UUID memberId, Long childId, Long diaryId) {
        log.info("Deleting diary {} for child: {}", diaryId, childId);
        childAccessValidator.validateDeleteAccess(memberId, childId);

        ChildDiary diary = diaryMapper.findActiveDiaryById(childId, diaryId)
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.NOT_FOUND));

        diary.setDeleteYn("Y");
        diary.setDeleteId(memberId);
        diary.setDeleteDate(LocalDateTime.now());

        childDiaryRepository.save(diary);

        return ApiResponse.success("성장일지 삭제 성공", null);
    }

    public ApiResponse<DiarySummaryDto> getDailySummary(UUID memberId, Long childId, String date) {
        log.info("Fetching daily summary for child: {} on date: {}", childId, date);
        childAccessValidator.validateReadAccess(memberId, childId);

        // 자녀 정보 조회
        Child child = childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        // 개월수 계산
        int months = calculateMonths(child.getBirthDay());
        String division = months < 12 ? "newborn" : "infant";

        log.info("Child {} is {} months old, using division: {}", childId, months, division);

        // 해당 division의 모든 항목 조회 (기록이 없으면 0으로 표시)
        List<DiarySummaryDto.ItemSummary> items = diaryMapper.findDailySummaryByDivision(childId, date, division);

        DiarySummaryDto data = DiarySummaryDto.builder()
                .childId(childId)
                .date(date)
                .items(items)
                .build();

        return ApiResponse.success("일지 요약 조회 성공", data);
    }

    private int calculateMonths(String birthDay) {
        if (birthDay == null || birthDay.isBlank()) {
            return 0;
        }

        try {
            LocalDate birth = LocalDate.parse(birthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate now = LocalDate.now();
            Period period = Period.between(birth, now);
            return period.getYears() * 12 + period.getMonths();
        } catch (Exception e) {
            log.warn("Failed to parse birth day: {}", birthDay, e);
            return 0;
        }
    }

    private Map<Long, CcDiaryItem> getItemMap() {
        return diaryMapper.findAllActiveItems().stream()
                .collect(Collectors.toMap(CcDiaryItem::getCcDiSeq, Function.identity()));
    }

    private ChildDiaryDto toDto(ChildDiary diary, CcDiaryItem item) {
        return ChildDiaryDto.builder()
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

    /**
     * 기간별 일지 통계 조회
     * @param memberId 회원 ID
     * @param childId 자녀 ID
     * @param periodType 기간 타입 (week, month, year)
     * @param startDate 시작일 (YYYY-MM-DD)
     * @param endDate 종료일 (YYYY-MM-DD)
     */
    public ApiResponse<DiaryStatDto> getDiaryStats(UUID memberId, Long childId, String periodType, String startDate, String endDate) {
        log.info("Fetching diary stats for child: {} periodType: {} from {} to {}", childId, periodType, startDate, endDate);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<DiaryStatDto.DiaryStat> stats = diaryMapper.findDiaryStatsByPeriod(childId, periodType, startDate, endDate);

        DiaryStatDto data = DiaryStatDto.builder()
                .childId(childId)
                .periodType(periodType)
                .startDate(startDate)
                .endDate(endDate)
                .stats(stats)
                .build();

        return ApiResponse.success("일지 통계 조회 성공", data);
    }

    // ==================== 메모 관련 메서드 ====================

    /**
     * 특정 날짜의 메모 조회
     */
    public ApiResponse<DiaryMemoDto> getMemo(UUID memberId, Long childId, String date) {
        log.info("Fetching memo for child: {} on date: {}", childId, date);
        childAccessValidator.validateReadAccess(memberId, childId);

        ChildDiaryMemo memo = diaryMapper.findActiveMemoByChildAndDate(childId, date)
                .orElse(null);

        if (memo == null) {
            return ApiResponse.success("메모 조회 성공", null);
        }

        return ApiResponse.success("메모 조회 성공", toMemoDto(memo));
    }

    /**
     * 메모 생성
     */
    @Transactional
    public ApiResponse<DiaryMemoDto> createMemo(UUID memberId, Long childId, DiaryMemoRequest request) {
        log.info("Creating memo for child: {} on date: {}", childId, request.getDate());
        childAccessValidator.validateWriteAccess(memberId, childId);

        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new DiaryException(DiaryErrorCode.DATE_REQUIRED);
        }

        // 해당 날짜에 이미 메모가 있는지 확인
        ChildDiaryMemo existingMemo = diaryMapper.findActiveMemoByChildAndDate(childId, request.getDate())
                .orElse(null);

        if (existingMemo != null) {
            throw new DiaryException(DiaryErrorCode.MEMO_ALREADY_EXISTS);
        }

        ChildDiaryMemo memo = ChildDiaryMemo.builder()
                .chSeq(childId)
                .dmDate(request.getDate())
                .memo(request.getMemo())
                .regId(memberId)
                .build();

        diaryMapper.insertMemo(memo);

        ChildDiaryMemo savedMemo = diaryMapper.findActiveMemoById(childId, memo.getDmSeq())
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.MEMO_NOT_FOUND));

        return ApiResponse.success("메모 등록 성공", toMemoDto(savedMemo));
    }

    /**
     * 메모 수정
     */
    @Transactional
    public ApiResponse<DiaryMemoDto> updateMemo(UUID memberId, Long childId, Long memoId, DiaryMemoRequest request) {
        log.info("Updating memo {} for child: {}", memoId, childId);
        childAccessValidator.validateWriteAccess(memberId, childId);

        ChildDiaryMemo memo = diaryMapper.findActiveMemoById(childId, memoId)
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.MEMO_NOT_FOUND));

        String newDate = (request.getDate() != null && !request.getDate().isBlank())
                ? request.getDate() : memo.getDmDate();

        diaryMapper.updateMemo(childId, memoId, newDate, request.getMemo());

        ChildDiaryMemo updatedMemo = diaryMapper.findActiveMemoById(childId, memoId)
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.MEMO_NOT_FOUND));

        return ApiResponse.success("메모 수정 성공", toMemoDto(updatedMemo));
    }

    /**
     * 메모 삭제
     */
    @Transactional
    public ApiResponse<Void> deleteMemo(UUID memberId, Long childId, Long memoId) {
        log.info("Deleting memo {} for child: {}", memoId, childId);
        childAccessValidator.validateDeleteAccess(memberId, childId);

        diaryMapper.findActiveMemoById(childId, memoId)
                .orElseThrow(() -> new DiaryException(DiaryErrorCode.MEMO_NOT_FOUND));

        diaryMapper.softDeleteMemo(childId, memoId, memberId);

        return ApiResponse.success("메모 삭제 성공", null);
    }

    private DiaryMemoDto toMemoDto(ChildDiaryMemo memo) {
        return DiaryMemoDto.builder()
                .id(memo.getDmSeq())
                .childId(memo.getChSeq())
                .date(memo.getDmDate())
                .memo(memo.getMemo())
                .build();
    }
}
