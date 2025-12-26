package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bi_seq")
    private Long biSeq;

    @Column(name = "bo_seq", nullable = false)
    private Long boSeq;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "read_count")
    private Integer readCount;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "fix_yn", length = 1)
    private String fixYn;

    @Column(name = "reg_user_postcode")
    private Integer regUserPostcode;

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
