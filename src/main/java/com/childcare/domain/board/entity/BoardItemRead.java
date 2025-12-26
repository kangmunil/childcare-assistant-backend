package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_item_read", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bi_seq", "mb_id"})
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

    @Column(name = "mb_id", nullable = false, columnDefinition = "uuid")
    private UUID mbId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
