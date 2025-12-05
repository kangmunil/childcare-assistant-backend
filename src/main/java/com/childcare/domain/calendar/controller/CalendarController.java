package com.childcare.domain.calendar.controller;

import com.childcare.domain.calendar.dto.CalendarDto;
import com.childcare.domain.calendar.dto.CalendarRequest;
import com.childcare.domain.calendar.service.CalendarService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/children/{childId}/calendars")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarDto>>> getCalendars(
            @PathVariable Long childId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        Long memberSeq = getMemberSeq();
        log.info("Get calendars request for child: {}, date: {}, month: {}", childId, date, month);

        ApiResponse<List<CalendarDto>> response;
        if (date != null && !date.isBlank()) {
            response = calendarService.getCalendarsByChildAndDate(memberSeq, childId, date);
        } else if (month != null && !month.isBlank()) {
            response = calendarService.getCalendarsByChildAndMonth(memberSeq, childId, month);
        } else {
            response = calendarService.getCalendarsByChild(memberSeq, childId);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CalendarDto>> createCalendar(
            @PathVariable Long childId,
            @RequestBody CalendarRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Create calendar request for child: {}", childId);

        ApiResponse<CalendarDto> response = calendarService.createCalendar(memberSeq, childId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarDto>> updateCalendar(
            @PathVariable Long childId,
            @PathVariable Long calendarId,
            @RequestBody CalendarRequest request) {
        Long memberSeq = getMemberSeq();
        log.info("Update calendar {} request for child: {}", calendarId, childId);

        ApiResponse<CalendarDto> response = calendarService.updateCalendar(memberSeq, childId, calendarId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendar(
            @PathVariable Long childId,
            @PathVariable Long calendarId) {
        Long memberSeq = getMemberSeq();
        log.info("Delete calendar {} request for child: {}", calendarId, childId);

        ApiResponse<Void> response = calendarService.deleteCalendar(memberSeq, childId, calendarId);
        return ResponseEntity.ok(response);
    }

    private Long getMemberSeq() {
        return SecurityUtil.getCurrentMemberSeq();
    }
}
