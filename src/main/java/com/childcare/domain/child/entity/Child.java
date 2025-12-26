package com.childcare.domain.child.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ch_seq")
    private Long chSeq;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_day", nullable = false)
    private String birthDay;

    @Column(name = "birth_time", nullable = false)
    private String birthTime;

    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Column(name = "height", length = 50)
    private String height;

    @Column(name = "weight", length = 50)
    private String weight;

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
