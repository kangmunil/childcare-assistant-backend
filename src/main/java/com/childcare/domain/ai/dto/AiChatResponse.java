package com.childcare.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    
    private String reply;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    private String timestamp;

    private AiChatMeta meta;
}
