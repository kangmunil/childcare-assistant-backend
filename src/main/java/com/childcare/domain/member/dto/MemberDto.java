package com.childcare.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    private Long id;
    private String memberId;
    private String name;
    private String phone;
    private String tel;
    private String email;
    private String postcode;
    private String addr1;
    private String addr2;
    private String inviteCode;
}
