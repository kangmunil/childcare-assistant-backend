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
public class ChildResponse {

    private String status;
    private String message;
    private List<ChildDto> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildDto {
        private Long id;
        private String name;
        private String birthDate;
        private String gender;
        private String photoUrl;
    }
}
