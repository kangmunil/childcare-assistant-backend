package com.childcare.domain.parent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
