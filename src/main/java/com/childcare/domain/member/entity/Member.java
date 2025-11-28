package com.childcare.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    
    @Column(name = "addr1")
    private String addr1;
    
    @Column(name = "addr2")
    private String addr2;
    
    @Column(name = "addr3", length = 1000)
    private String addr3;
    
    @Column(name = "login_date")
    private LocalDateTime loginDate;
    
    @Column(name = "leave_date")
    private LocalDateTime leaveDate;
    
    @CreatedDate
    @Column(name = "reg_date", nullable = false, updatable = false)
    private LocalDateTime regDate;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MemberOAuth> oAuthConnections;
}