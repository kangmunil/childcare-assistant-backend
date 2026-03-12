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
public class AiChatQuickAction {

    private String id;

    private String label;

    @JsonProperty("action_type")
    private String actionType;

    private String route;

    private String query;

    @JsonProperty("intent_hint")
    private String intentHint;

    @JsonProperty("requested_profile_domains")
    private List<String> requestedProfileDomains;
}
