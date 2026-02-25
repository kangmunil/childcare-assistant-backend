package com.childcare.domain.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProfileSummaryDto {
    private Long childId;
    private String summaryText;
    private Integer summaryVersion;
    private OffsetDateTime updatedAt;
}
