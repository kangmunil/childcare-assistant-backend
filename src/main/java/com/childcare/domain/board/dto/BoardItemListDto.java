package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardItemListDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer readCount;
    private Integer likeCount;
    private Integer commentCount;
    private String fixYn;
    private boolean liked;

    // 작성자 정보
    private UUID regId;
    private String regUserName;
    private String regUserRegionName;
    private LocalDateTime regDate;

    // 첨부파일 여부
    private boolean hasFile;
    private String thumbnailUrl;

    // 인기글 여부 (조회수+공감수 상위 3건)
    private boolean isPopular;
}
