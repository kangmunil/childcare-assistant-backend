package com.childcare.domain.checklist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cc_grow_checklist_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcGrowChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cc_grow_checklist_item_seq")
    @SequenceGenerator(name = "cc_grow_checklist_item_seq", sequenceName = "cc_grow_checklist_item_cc_gc_seq", allocationSize = 1)
    @Column(name = "cc_gc_seq")
    private Long ccGcSeq;

    @Column(name = "cc_gc_code", nullable = false)
    private String ccGcCode;

    @Column(name = "cc_gc_content", nullable = false)
    private String ccGcContent;

    @Column(name = "cc_gc_start_month", nullable = false, length = 10)
    private String ccGcStartMonth;

    @Column(name = "cc_gc_end_month", nullable = false, length = 10)
    private String ccGcEndMonth;

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
