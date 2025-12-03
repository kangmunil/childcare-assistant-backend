package com.childcare.domain.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiarySummaryDto {
    private Long childId;
    private String date;
    private List<ItemSummary> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemSummary {
        private Long itemId;
        private String itemDivision;
        private String itemCode;
        private String itemName;
        private String unit;
        private Double totalAmount;
        private Integer count;
    }
}
