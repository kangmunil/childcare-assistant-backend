package com.childcare.domain.board.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCommentDto {
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
    private LocalDateTime regDate;

    // 수정 정보
    private UUID updateId;
    private LocalDateTime updateDate;

    // 대댓글 목록
    private List<BoardCommentDto> replies;

    // 현재 사용자가 공감했는지 여부
    private boolean liked;

    // 현재 사용자가 작성자인지 여부
    @JsonProperty("isAuthor")
    private boolean isAuthor;

    // 비밀댓글 접근 가능 여부
    private boolean accessible;
}
