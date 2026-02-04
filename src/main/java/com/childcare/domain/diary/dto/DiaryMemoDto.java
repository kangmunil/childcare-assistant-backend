package com.childcare.domain.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryMemoDto {
    private Long id;
    private Long childId;
    private String date;
    private String memo;
}
