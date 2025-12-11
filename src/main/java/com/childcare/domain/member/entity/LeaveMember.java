package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveMember {

    @Id
    @Column(name = "mb_seq")
    private Long mbSeq;

    @Column(name = "id")
    private String id;

    @Column(name = "password")
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "tel")
    private String tel;

    @Column(name = "email")
    private String email;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "addr1")
    private String addr1;

    @Column(name = "addr2", length = 1000)
    private String addr2;

    @Column(name = "invite_code")
    private String inviteCode;

    @Column(name = "invite_by")
    private Long invitedBy;

    @Column(name = "login_date")
    private LocalDateTime loginDate;

    @Column(name = "leave_date", nullable = false)
    private LocalDateTime leaveDate;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;
}
