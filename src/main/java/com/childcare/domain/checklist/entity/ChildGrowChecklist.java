package com.childcare.domain.checklist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_grow_checklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildGrowChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gc_seq")
    private Long gcSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "cc_gc_seq", nullable = false)
    private Long ccGcSeq;

    @Column(name = "gc_date", nullable = false, length = 20)
    private String gcDate;

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
