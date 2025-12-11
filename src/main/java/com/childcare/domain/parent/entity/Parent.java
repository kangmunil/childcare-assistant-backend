package com.childcare.domain.parent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Integer paSeq;

    @Column(name = "mb_seq", nullable = false)
    private Long mbSeq;

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

    @Column(name = "reg_user_seq", nullable = false)
    private Long regUserSeq;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
