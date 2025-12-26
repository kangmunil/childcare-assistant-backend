package com.childcare.domain.board.dto;

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
public class BoardItemDto {
    private Long id;
    private Long boardId;
    private String boardTitle;
    private String title;
    private String content;
    private Integer readCount;
    private Integer likeCount;
    private String fixYn;
    private Integer regUserPostcode;

    // 작성자 정보
    private UUID regId;
    private String regUserName;
    private LocalDateTime regDate;

    // 수정 정보
    private UUID updateId;
    private LocalDateTime updateDate;

    // 첨부파일
    private List<BoardFileDto> files;

    // 댓글 수
    private Integer commentCount;

    // 현재 사용자가 공감했는지 여부
    private boolean liked;

    // 현재 사용자가 작성자인지 여부
    private boolean isAuthor;
}
