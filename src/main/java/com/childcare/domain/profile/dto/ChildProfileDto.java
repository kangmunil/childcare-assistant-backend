package com.childcare.domain.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChildProfileDto {
    private Long childId;
    private Map<String, Object> health;
    private Map<String, Object> routine;
    private Map<String, Object> development;
    private Map<String, Object> education;
    private Map<String, Object> safety;
    private Map<String, Object> misc;
    private OffsetDateTime updatedAt;
}
