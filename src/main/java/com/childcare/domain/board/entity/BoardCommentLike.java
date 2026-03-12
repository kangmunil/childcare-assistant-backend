package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_comment_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bc_seq", "mb_id"})
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

    @Column(name = "mb_id", nullable = false, columnDefinition = "uuid")
    private UUID mbId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
