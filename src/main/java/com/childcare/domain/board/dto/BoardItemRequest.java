package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardItemRequest {
    private String title;
    private String content;
    private String category;
    private String fixYn; // 상단 고정 여부 (Y/N) - ADMIN만 설정 가능
    @Deprecated
    private String locationScope; // deprecated write alias for postScope ('all' or 'neighbor')
    private String postScope; // community post scope ('all' or 'neighbor')

    // 장소(병원/기관) 정보 추가
    private String placeName;
    private String placeAddress;
    private Double placeLat;
    private Double placeLng;
}
