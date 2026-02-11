package com.childcare.domain.member.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class MemberUpdateRequest {

    private String name;

    private String phone;

    private String tel;

    @Email(message = "Invalid email format")
    private String email;

    private String postcode;

    private String addr1;

    private String addr2;

    private String regionName;
}
