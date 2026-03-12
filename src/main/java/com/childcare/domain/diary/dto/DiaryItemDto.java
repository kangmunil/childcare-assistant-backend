package com.childcare.domain.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryItemDto {
    private Long id;
    private String division;
    private String code;
    private String name;
    private String unit;
}
