package com.childcare.domain.auth.dto;

import lombok.Data;

@Data
public class GoogleUserInfo {
    
    private String sub;
    private String email;
    private String name;
    private String picture;
    private String given_name;
    private String family_name;
    private Boolean email_verified;
}