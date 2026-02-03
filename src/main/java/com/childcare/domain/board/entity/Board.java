package com.childcare.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bo_seq")
    private Long boSeq;

    @Column(name = "bo_code", nullable = false)
    private String boCode;

    @Column(name = "bo_slug", length = 50, unique = true)
    private String boSlug;

    @Column(name = "bo_title", nullable = false)
    private String boTitle;

    @Column(name = "bo_desc", length = 1000)
    private String boDesc;

    @Column(name = "bo_type", length = 1)
    private String boType;

    @Column(name = "bo_use_yn", length = 1)
    private String boUseYn;

    @Column(name = "bo_read_auth", length = 20)
    private String boReadAuth;

    @Column(name = "bo_write_auth", length = 20)
    private String boWriteAuth;

    @Column(name = "bo_delete_auth", length = 20)
    private String boDeleteAuth;

    @Column(name = "bo_file_count", length = 20)
    private String boFileCount;

    @Column(name = "bo_file_extension", length = 500)
    private String boFileExtension;

    @Column(name = "bo_file_size")
    private Integer boFileSize;

    @Column(name = "bo_neighbor_yn", length = 1)
    private String boNeighborYn;
}
