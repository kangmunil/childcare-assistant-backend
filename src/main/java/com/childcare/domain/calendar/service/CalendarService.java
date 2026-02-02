package com.childcare.domain.calendar.service;

import com.childcare.domain.calendar.dto.CalendarDto;
import com.childcare.domain.calendar.dto.CalendarRequest;
import com.childcare.domain.calendar.entity.Calendar;
import com.childcare.domain.calendar.mapper.CalendarMapper;
import com.childcare.domain.calendar.repository.CalendarRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.CalendarException;
import com.childcare.global.exception.CalendarException.CalendarErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarMapper calendarMapper;
    private final ChildAccessValidator childAccessValidator;

    public ApiResponse<List<CalendarDto>> getCalendarsByChild(UUID memberId, Long childId) {
        log.info("Fetching calendars for child: {}", childId);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<Calendar> calendars = calendarMapper.findCalendarsByChildId(childId);

        List<CalendarDto> calendarDtos = calendars.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("일정 조회 성공", calendarDtos);
    }

    public ApiResponse<List<CalendarDto>> getCalendarsByChildAndDate(UUID memberId, Long childId, String date) {
        log.info("Fetching calendars for child: {} on date: {}", childId, date);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<Calendar> calendars = calendarMapper.findCalendarsByChildIdAndDate(childId, date);

        List<CalendarDto> calendarDtos = calendars.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("일정 조회 성공", calendarDtos);
    }

    public ApiResponse<List<CalendarDto>> getCalendarsByChildAndMonth(UUID memberId, Long childId, String yearMonth) {
        log.info("Fetching calendars for child: {} on month: {}", childId, yearMonth);
        childAccessValidator.validateReadAccess(memberId, childId);

        List<Calendar> calendars = calendarMapper.findCalendarsByChildIdAndMonth(childId, yearMonth);

        List<CalendarDto> calendarDtos = calendars.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("일정 조회 성공", calendarDtos);
    }

    public ApiResponse<CalendarDto> getCalendar(UUID memberId, Long childId, Long calendarId) {
        log.info("Fetching calendar {} for child: {}", calendarId, childId);
        childAccessValidator.validateReadAccess(memberId, childId);

        Calendar calendar = calendarMapper.findActiveCalendarById(childId, calendarId)
                .orElseThrow(() -> new CalendarException(CalendarErrorCode.NOT_FOUND));

        return ApiResponse.success("일정 조회 성공", toDto(calendar));
    }

    @Transactional
    public ApiResponse<CalendarDto> createCalendar(UUID memberId, Long childId, CalendarRequest request) {
        log.info("Creating calendar for child: {}", childId);
        childAccessValidator.validateWriteAccess(memberId, childId);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new CalendarException(CalendarErrorCode.TITLE_REQUIRED);
        }
        if (request.getCaDate() == null || request.getCaDate().isBlank()) {
            throw new CalendarException(CalendarErrorCode.DATE_REQUIRED);
        }
        if (request.getCaTime() == null || request.getCaTime().isBlank()) {
            throw new CalendarException(CalendarErrorCode.TIME_REQUIRED);
        }

        Calendar calendar = Calendar.builder()
                .chSeq(childId)
                .div(request.getDiv())
                .title(request.getTitle())
                .caDate(request.getCaDate())
                .caTime(request.getCaTime())
                .place(request.getPlace())
                .placePostcode(request.getPlacePostcode())
                .placeAddress1(request.getPlaceAddress1())
                .placeAddress2(request.getPlaceAddress2())
                .memo(request.getMemo())
                .regAiYn("N")
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .deleteYn("N")
                .build();

        Calendar savedCalendar = calendarRepository.save(calendar);

        return ApiResponse.success("일정 등록 성공", toDto(savedCalendar));
    }

    @Transactional
    public ApiResponse<CalendarDto> updateCalendar(UUID memberId, Long childId, Long calendarId, CalendarRequest request) {
        log.info("Updating calendar {} for child: {}", calendarId, childId);
        childAccessValidator.validateWriteAccess(memberId, childId);

        Calendar calendar = calendarMapper.findActiveCalendarById(childId, calendarId)
                .orElseThrow(() -> new CalendarException(CalendarErrorCode.NOT_FOUND));

        calendar.setDiv(request.getDiv());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            calendar.setTitle(request.getTitle());
        }
        if (request.getCaDate() != null && !request.getCaDate().isBlank()) {
            calendar.setCaDate(request.getCaDate());
        }
        if (request.getCaTime() != null && !request.getCaTime().isBlank()) {
            calendar.setCaTime(request.getCaTime());
        }
        calendar.setPlace(request.getPlace());
        calendar.setPlacePostcode(request.getPlacePostcode());
        calendar.setPlaceAddress1(request.getPlaceAddress1());
        calendar.setPlaceAddress2(request.getPlaceAddress2());
        calendar.setMemo(request.getMemo());

        Calendar updatedCalendar = calendarRepository.save(calendar);

        return ApiResponse.success("일정 수정 성공", toDto(updatedCalendar));
    }

    @Transactional
    public ApiResponse<Void> deleteCalendar(UUID memberId, Long childId, Long calendarId) {
        log.info("Deleting calendar {} for child: {}", calendarId, childId);
        childAccessValidator.validateDeleteAccess(memberId, childId);

        Calendar calendar = calendarMapper.findActiveCalendarById(childId, calendarId)
                .orElseThrow(() -> new CalendarException(CalendarErrorCode.NOT_FOUND));

        calendar.setDeleteYn("Y");
        calendar.setDeleteId(memberId);
        calendar.setDeleteDate(LocalDateTime.now());

        calendarRepository.save(calendar);

        return ApiResponse.success("일정 삭제 성공", null);
    }

    private CalendarDto toDto(Calendar calendar) {
        return CalendarDto.builder()
                .id(calendar.getCaSeq())
                .childId(calendar.getChSeq())
                .div(calendar.getDiv())
                .title(calendar.getTitle())
                .caDate(calendar.getCaDate())
                .caTime(calendar.getCaTime())
                .place(calendar.getPlace())
                .placePostcode(calendar.getPlacePostcode())
                .placeAddress1(calendar.getPlaceAddress1())
                .placeAddress2(calendar.getPlaceAddress2())
                .memo(calendar.getMemo())
                .build();
    }
}
