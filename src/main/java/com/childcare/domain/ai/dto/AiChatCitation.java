package com.childcare.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatCitation {

    private String label;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("basis_date")
    private String basisDate;

    private String note;

    private String url;
}
