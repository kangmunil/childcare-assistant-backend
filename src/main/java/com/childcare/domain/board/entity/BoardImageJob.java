package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_image_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardImageJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bij_seq")
    private Long bijSeq;

    @Column(name = "bia_seq", nullable = false)
    private Long biaSeq;

    @Column(name = "job_type", nullable = false, length = 20)
    private String jobType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_error", length = 4000)
    private String lastError;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "upd_date", nullable = false)
    private LocalDateTime updDate;
}
