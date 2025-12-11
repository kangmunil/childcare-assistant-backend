package com.childcare.domain.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistDto {
    private Long childId;
    private Long itemId;
    private String itemCode;
    private String itemContent;
    private String startMonth;
    private String endMonth;
}
