package com.childcare.domain.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProfilePatchRequest {
    private Map<String, Object> health;
    private Map<String, Object> routine;
    private Map<String, Object> development;
    private Map<String, Object> education;
    private Map<String, Object> safety;
    private Map<String, Object> misc;
}
