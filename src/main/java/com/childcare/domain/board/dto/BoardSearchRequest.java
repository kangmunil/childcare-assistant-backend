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
    private String category;
    private Integer page;        // 페이지 번호 (0부터 시작)
    private Integer size;        // 페이지 크기 (기본값: 20)
    private Boolean includeHighlights; // 고정글/인기글 포함 여부 (기본값: true)
    private String locationScope; // all, neighbor

    public int getPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int getSize() {
        return size == null || size <= 0 ? 20 : size;
    }

    public boolean isIncludeHighlights() {
        return includeHighlights == null || includeHighlights;
    }

    public String getLocationScope() {
        if (locationScope == null || locationScope.isBlank()) {
            return "all";
        }
        String normalized = locationScope.trim().toLowerCase();
        return "neighbor".equals(normalized) ? "neighbor" : "all";
    }

    public boolean isNeighborLocationScope() {
        return "neighbor".equals(getLocationScope());
    }
}
