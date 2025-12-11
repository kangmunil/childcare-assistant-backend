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
public class DiaryStatDto {
    private Long childId;
    private String periodType;  // week, month, year
    private String startDate;
    private String endDate;
    private List<DiaryStat> stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryStat {
        private String week;        // 주 시작일 (YYYY-MM-DD)
        private String itemName;    // 항목명
        private Double totalAmount; // 합계
    }
}
