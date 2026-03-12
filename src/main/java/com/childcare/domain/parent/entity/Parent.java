package com.childcare.domain.parent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pa_seq")
    private Long paSeq;

    @Column(name = "mb_id", nullable = false, columnDefinition = "uuid")
    private UUID mbId;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "relation", nullable = false)
    private String relation;

    @Column(name = "auth_manage", length = 1)
    @Builder.Default
    private String authManage = "1";

    @Column(name = "auth_read", length = 1)
    @Builder.Default
    private String authRead = "1";

    @Column(name = "auth_write", length = 1)
    @Builder.Default
    private String authWrite = "1";

    @Column(name = "auth_delete", length = 1)
    @Builder.Default
    private String authDelete = "1";

    @Column(name = "reg_id", nullable = false, columnDefinition = "uuid")
    private UUID regId;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
