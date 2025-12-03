package com.childcare.domain.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildRequest {

    private String name;
    private String birthDay;
    private String birthTime;
    private String gender;
    private Double height;
    private Double weight;
    private String memo;
}
