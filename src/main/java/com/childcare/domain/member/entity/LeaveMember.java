package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveMember {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

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

    @Column(name = "invited_by", columnDefinition = "uuid")
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "login_date")
    private LocalDateTime loginDate;

    @Column(name = "leave_date", nullable = false)
    private LocalDateTime leaveDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
