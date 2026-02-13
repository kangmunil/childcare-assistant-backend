package com.childcare.domain.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDto {
    private Long id;
    private String name;
    private String birthDate;
    private String birthTime;
    private String gender;
    private String height;
    private String weight;
    private String photoUrl;
    private String ownerName;
    private Boolean isOwner;
}
