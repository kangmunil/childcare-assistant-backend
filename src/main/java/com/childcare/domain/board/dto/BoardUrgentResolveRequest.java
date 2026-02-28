package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardUrgentResolveRequest {
    private Boolean resolved; // null -> true

    public boolean isResolved() {
        return resolved == null || resolved;
    }
}
