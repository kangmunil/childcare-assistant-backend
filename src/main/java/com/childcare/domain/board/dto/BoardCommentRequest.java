package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCommentRequest {
    private Long parentSeq;    // 부모 댓글 ID (대댓글인 경우)
    private String content;
    private String secretYn;   // 비밀댓글 여부 (Y/N)
    private String fixYn;      // 상단 고정 여부 (Y/N) - ADMIN만 설정 가능
}
