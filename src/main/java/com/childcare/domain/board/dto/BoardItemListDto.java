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
    private String postScope;
    private Integer readCount;
    private Integer likeCount;
    private Integer commentCount;
    private String fixYn;
    private boolean liked;

    // 장소(병원/기관) 정보 추가
    private String placeName;
    private String placeAddress;
    private Double placeLat;
    private Double placeLng;
    private boolean urgentResolved;
    private LocalDateTime urgentResolvedDate;

    // 작성자 정보
    private UUID regId;
    private String regUserName;
    private String regUserRegionName;
    private String regUserRegionDongLabel;
    private String regUserParentingStage;
    private Boolean regUserHonorNeighbor;
    private boolean sameNeighborhood;
    private LocalDateTime regDate;

    // 첨부파일 여부
    private boolean hasFile;
    private String thumbnailUrl;
    private String thumbnailAvifUrl;
    private String thumbnailWebpUrl;
    private String thumbnailJpegUrl;
    private String thumbnailPngUrl;
    private Integer thumbnailWidth;
    private Integer thumbnailHeight;

    // 인기글 여부 (조회수+공감수 상위 3건)
    private boolean isPopular;

    // 게시판 정보
    private String boardSlug;
    private String boardTitle;
}
