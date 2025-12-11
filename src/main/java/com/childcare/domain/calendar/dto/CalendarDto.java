package com.childcare.domain.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDto {
    private Long id;
    private Long childId;
    private String div;
    private String title;
    private String caDate;
    private String caTime;
    private String place;
    private String placePostcode;
    private String placeAddress1;
    private String placeAddress2;
    private String memo;
}
