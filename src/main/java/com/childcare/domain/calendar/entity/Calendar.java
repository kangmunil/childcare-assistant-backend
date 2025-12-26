package com.childcare.domain.calendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_calendar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ca_seq")
    private Long caSeq;

    @Column(name = "ch_seq", nullable = false)
    private Long chSeq;

    @Column(name = "div")
    private String div;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "ca_date", nullable = false)
    private String caDate;

    @Column(name = "ca_time", nullable = false)
    private String caTime;

    @Column(name = "place")
    private String place;

    @Column(name = "place_postcode")
    private String placePostcode;

    @Column(name = "place_address1")
    private String placeAddress1;

    @Column(name = "place_address2")
    private String placeAddress2;

    @Column(name = "memo")
    private String memo;

    @Column(name = "reg_ai_yn")
    private String regAiYn;

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
