package com.childcare.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "ID is required")
    @Size(min = 4, max = 20, message = "ID must be between 4 and 20 characters")
    private String id;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String tel;
    private String addr1;
    private String addr2;
    private String addr3;
    private String referralCode;  // 추천인 초대코드 (선택)
}