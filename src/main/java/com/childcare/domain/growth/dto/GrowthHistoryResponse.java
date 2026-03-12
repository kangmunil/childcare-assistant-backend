package com.childcare.domain.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthHistoryResponse {
    private Long id;
    private Long childId;
    private String height;
    private String weight;
    private String ghDate;  // YYYY-MM-DD
    private LocalDateTime recordedAt;
}
