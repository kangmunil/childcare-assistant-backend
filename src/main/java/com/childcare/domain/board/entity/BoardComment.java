package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_comment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bc_seq")
    private Long bcSeq;

    @Column(name = "bi_seq", nullable = false)
    private Long biSeq;

    @Column(name = "parent_seq")
    private Long parentSeq;

    @Column(name = "depth")
    private Integer depth;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "secret_yn", length = 1)
    private String secretYn;

    @Column(name = "fix_yn", length = 1)
    private String fixYn;

    @Column(name = "reg_id", nullable = false, columnDefinition = "uuid")
    private UUID regId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "update_id", columnDefinition = "uuid")
    private UUID updateId;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "delete_yn", length = 1)
    private String deleteYn;

    @Column(name = "delete_id", columnDefinition = "uuid")
    private UUID deleteId;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;
}
