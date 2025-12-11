package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_item_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bi_seq", "mb_seq"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardItemLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bil_seq")
    private Long bilSeq;

    @Column(name = "bi_seq", nullable = false)
    private Long biSeq;

    @Column(name = "mb_seq", nullable = false)
    private Long mbSeq;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
