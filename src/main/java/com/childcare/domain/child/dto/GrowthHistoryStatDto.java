package com.childcare.domain.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthHistoryStatDto {
    private Long childId;
    private String periodType;  // day, week, month, year
    private String startDate;
    private String endDate;
    private List<GrowthStat> stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthStat {
        private String period;
        private String height;
        private String weight;
    }
}
