package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_member_oauth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveMemberOAuth {

    @Id
    @Column(name = "oauth_seq")
    private Long oauthSeq;

    @Column(name = "mb_seq", nullable = false)
    private Long mbSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private MemberOAuth.OAuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "connect_date", nullable = false)
    private LocalDateTime connectDate;

    @Column(name = "leave_date", nullable = false)
    private LocalDateTime leaveDate;
}
