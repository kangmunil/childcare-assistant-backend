package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_image_variant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardImageVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biv_seq")
    private Long bivSeq;

    @Column(name = "bia_seq", nullable = false)
    private Long biaSeq;

    @Column(name = "variant_role", nullable = false, length = 20)
    private String variantRole;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
