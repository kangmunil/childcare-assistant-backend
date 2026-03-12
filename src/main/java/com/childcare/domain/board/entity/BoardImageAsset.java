package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_image_asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardImageAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bia_seq")
    private Long biaSeq;

    @Column(name = "bf_seq", nullable = false)
    private Long bfSeq;

    @Column(name = "master_bucket", nullable = false, length = 200)
    private String masterBucket;

    @Column(name = "master_path", nullable = false, length = 1000)
    private String masterPath;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "has_alpha")
    private Boolean hasAlpha;

    @Column(name = "is_animated")
    private Boolean isAnimated;

    @Column(name = "optimization_status", nullable = false, length = 20)
    private String optimizationStatus;

    @Column(name = "last_error", length = 4000)
    private String lastError;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "upd_date", nullable = false)
    private LocalDateTime updDate;
}
