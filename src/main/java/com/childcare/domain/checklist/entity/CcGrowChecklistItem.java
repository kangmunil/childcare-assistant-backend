package com.childcare.domain.checklist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cc_grow_checklist_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcGrowChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cc_gc_seq")
    private Long ccGcSeq;

    @Column(name = "cc_gc_div", nullable = false)
    private String ccGcDiv;

    @Column(name = "cc_gc_code", nullable = false)
    private String ccGcCode;

    @Column(name = "cc_gc_content", nullable = false)
    private String ccGcContent;

    @Column(name = "cc_gc_start_month", nullable = false, length = 10)
    private String ccGcStartMonth;

    @Column(name = "cc_gc_end_month", nullable = false, length = 10)
    private String ccGcEndMonth;

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
