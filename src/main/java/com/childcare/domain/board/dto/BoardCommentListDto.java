package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MyBatis 댓글 조회용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCommentListDto {
    private Long id;
    private Long itemId;
    private Long parentSeq;
    private Integer depth;
    private String content;
    private Integer likeCount;
    private String secretYn;
    private String fixYn;
    private String deleteYn;

    // 작성자 정보
    private UUID regId;
    private String regUserName;
    private String regUserParentingStage;
    private Boolean regUserHonorNeighbor;
    private LocalDateTime regDate;

    // 수정 정보
    private UUID updateId;
    private LocalDateTime updateDate;

    // 현재 사용자가 공감했는지 여부 (SQL에서 계산)
    private boolean liked;
}
