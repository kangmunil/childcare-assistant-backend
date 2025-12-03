package com.childcare.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cc_diary_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcDiaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cc_di_seq")
    private Long ccDiSeq;

    @Column(name = "cc_di_div", nullable = false)
    private String ccDiDiv;

    @Column(name = "cc_di_code", nullable = false)
    private String ccDiCode;

    @Column(name = "cc_di_name", nullable = false)
    private String ccDiName;

    @Column(name = "cc_di_unit", length = 20)
    private String ccDiUnit;

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
