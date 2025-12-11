package com.childcare.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "child_diary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "child_diary_seq")
    @SequenceGenerator(name = "child_diary_seq", sequenceName = "child_diary_di_seq", allocationSize = 1)
    @Column(name = "di_seq")
    private Long diSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "cc_di_seq", nullable = false)
    private Long ccDiSeq;

    @Column(name = "di_date", nullable = false, length = 20)
    private String diDate;

    @Column(name = "di_time", nullable = false, length = 10)
    private String diTime;

    @Column(name = "amount", length = 10)
    private String amount;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Column(name = "reg_ai_yn", length = 1)
    private String regAiYn;

    @Column(name = "reg_user_seq", nullable = false)
    private Long regUserSeq;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "delete_yn", length = 1)
    private String deleteYn;

    @Column(name = "delete_user_seq")
    private String deleteUserSeq;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;
}
