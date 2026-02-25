package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private Long id;
    private String code;
    private String slug;
    private String title;
    private String description;
    private String type;
    private String neighborYn;
}
