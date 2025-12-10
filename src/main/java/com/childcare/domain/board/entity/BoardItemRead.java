package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_item_read", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bi_seq", "mb_seq"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardItemRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bir_seq")
    private Long birSeq;

    @Column(name = "bi_seq", nullable = false)
    private Long biSeq;

    @Column(name = "mb_seq", nullable = false)
    private Long mbSeq;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
