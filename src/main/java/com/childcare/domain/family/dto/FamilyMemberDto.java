package com.childcare.domain.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDto {
    private Long memberId;
    private String memberName;
    private String relation;
    private String authManage;
    private String authRead;
    private String authWrite;
    private String authDelete;
    private boolean isMe;
}
