package com.childcare.domain.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiaryDto {
    private Long id;
    private Long childId;
    private Long itemId;
    private String itemDivision;
    private String itemCode;
    private String itemName;
    private String diDate;
    private String diTime;
    private String amount;
    private String memo;
}
