package com.childcare.domain.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthHistoryRequest {
    private String height;
    private String weight;
    private String ghDate;  // YYYY-MM-DD
}
