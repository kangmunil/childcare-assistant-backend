package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {

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

    @Column(name = "login_date")
    private LocalDateTime loginDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "invite_code", unique = true)
    private String inviteCode;

    @Column(name = "invited_by", columnDefinition = "uuid")
    private UUID invitedBy;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "region_name", length = 50)
    private String regionName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}