package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchRequest {
    private String searchType;   // title, content, titleContent, author
    private String keyword;
    private Integer page;        // 페이지 번호 (0부터 시작)
    private Integer size;        // 페이지 크기 (기본값: 20)

    public int getPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int getSize() {
        return size == null || size <= 0 ? 20 : size;
    }
}
