package com.childcare.domain.child.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_image")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ci_seq")
    private Long ciSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "org_filename", nullable = false, length = 1000)
    private String orgFilename;

    @Column(name = "ci_name", nullable = false, length = 1000)
    private String ciName;

    @Column(name = "ci_path", nullable = false, length = 1000)
    private String ciPath;

    @Column(name = "ci_extension", length = 1000)
    private String ciExtension;

    @Column(name = "ci_size")
    private Integer ciSize;

    @Column(name = "reg_id", nullable = false, columnDefinition = "uuid")
    private UUID regId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
