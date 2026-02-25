package com.childcare.domain.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProfileRecord {
    private Long childId;
    private String health;
    private String routine;
    private String development;
    private String education;
    private String safety;
    private String misc;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}
