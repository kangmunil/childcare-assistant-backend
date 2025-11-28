package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_oauth", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MemberOAuth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_seq")
    private Long oauthSeq;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mb_seq", nullable = false)
    private Member member;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;
    
    @Column(name = "provider_id", nullable = false)
    private String providerId;
    
    @Column(name = "nickname", length = 100)
    private String nickname;
    
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;
    
    @CreatedDate
    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;
    
    public enum OAuthProvider {
        KAKAO, GOOGLE
    }
}