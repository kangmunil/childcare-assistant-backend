package com.childcare.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_diary_memo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiaryMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dm_seq")
    private Long dmSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "dm_date", nullable = false, length = 20)
    private String dmDate;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "reg_ai_yn", length = 1)
    private String regAiYn;

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
