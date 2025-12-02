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
public class DiaryItemResponse {

    private String status;
    private String code;
    private String message;
    private List<DiaryItemDto> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryItemDto {
        private Long id;
        private String division;
        private String code;
        private String name;
        private String unit;
    }
}
