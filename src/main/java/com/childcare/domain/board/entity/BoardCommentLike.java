package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_comment_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bc_seq", "mb_seq"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcl_seq")
    private Long bclSeq;

    @Column(name = "bc_seq", nullable = false)
    private Long bcSeq;

    @Column(name = "mb_seq", nullable = false)
    private Long mbSeq;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
