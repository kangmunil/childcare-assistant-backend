package com.childcare.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMeta {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("response_mode")
    private String responseMode;

    private String intent;

    private Double confidence;

    @JsonProperty("fallback_code")
    private String fallbackCode;

    private List<AiChatCitation> citations;

    @JsonProperty("follow_up_questions")
    private List<String> followUpQuestions;

    @JsonProperty("quick_actions")
    private List<AiChatQuickAction> quickActions;

    private AiChatClarification clarification;
}
