package com.childcare.domain.family.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDto {
    private UUID memberId;
    private String memberName;
    private String relation;
    private String authManage;
    private String authRead;
    private String authWrite;
    private String authDelete;
    @JsonProperty("isMe")
    private boolean isMe;
    @JsonProperty("isApproved")
    private boolean isApproved;
}
