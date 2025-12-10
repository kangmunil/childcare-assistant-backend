package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private Long regUserSeq;
    private String regUserName;
    private LocalDateTime regDate;

    // 수정 정보
    private Long updateUserSeq;
    private LocalDateTime updateDate;

    // 대댓글 목록
    private List<BoardCommentDto> replies;

    // 현재 사용자가 공감했는지 여부
    private boolean liked;

    // 현재 사용자가 작성자인지 여부
    private boolean isAuthor;

    // 비밀댓글 접근 가능 여부
    private boolean accessible;

    // 삭제된 댓글인 경우 표시용
    private boolean deleted;
}
