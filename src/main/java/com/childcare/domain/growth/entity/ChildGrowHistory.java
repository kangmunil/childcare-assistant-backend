package com.childcare.domain.growth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_grow_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildGrowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gh_seq")
    private Long ghSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "height", nullable = false, length = 20)
    private String height;

    @Column(name = "weight", nullable = false, length = 10)
    private String weight;

    @Column(name = "reg_id", nullable = false, columnDefinition = "uuid")
    private UUID regId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "delete_yn", length = 1)
    private String deleteYn;

    @Column(name = "delete_id", columnDefinition = "uuid")
    private UUID deleteId;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;
}
