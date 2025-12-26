package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_file")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bf_seq")
    private Long bfSeq;

    @Column(name = "bi_seq", nullable = false)
    private Long biSeq;

    @Column(name = "org_filename", nullable = false, length = 1000)
    private String orgFilename;

    @Column(name = "bf_name", nullable = false, length = 1000)
    private String bfName;

    @Column(name = "bf_path", nullable = false, length = 1000)
    private String bfPath;

    @Column(name = "bf_extension", length = 1000)
    private String bfExtension;

    @Column(name = "bf_size")
    private Integer bfSize;

    @Column(name = "reg_id", nullable = false, columnDefinition = "uuid")
    private UUID regId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
