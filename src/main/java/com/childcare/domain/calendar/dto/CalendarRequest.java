package com.childcare.domain.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarRequest {
    private String title;
    private String caDate;
    private String caTime;
    private String memo;
}
