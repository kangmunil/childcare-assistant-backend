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
    private String fixYn;  // 상단 고정 여부 (Y/N) - ADMIN만 설정 가능
}
