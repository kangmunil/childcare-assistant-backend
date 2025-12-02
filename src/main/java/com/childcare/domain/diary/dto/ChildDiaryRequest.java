package com.childcare.domain.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiaryRequest {

    private Long itemId;
    private String diDate;
    private String diTime;
    private String amount;
    private String memo;
}
