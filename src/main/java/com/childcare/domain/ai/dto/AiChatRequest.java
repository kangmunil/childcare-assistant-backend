package com.childcare.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotBlank(message = "메시지는 공백일 수 없습니다.")
    @Size(max = 2000, message = "메시지는 최대 2000자까지 입력할 수 있습니다.")
    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("child_id")
    private Long childId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("profile_context")
    @Size(max = 4000, message = "profile_context는 최대 4000자까지 입력할 수 있습니다.")
    private String profileContext;

    @JsonProperty("context_mode")
    @Pattern(regexp = "(?i)AUTO|MANUAL", message = "context_mode는 AUTO 또는 MANUAL이어야 합니다.")
    private String contextMode;

    @JsonProperty("intent_hint")
    private String intentHint;

    @JsonProperty("growth_context")
    private Map<String, Object> growthContext;

    @JsonProperty("requested_profile_domains")
    private List<String> requestedProfileDomains;

    public String getEffectiveContextMode() {
        if (contextMode == null || contextMode.isBlank()) {
            return "AUTO";
        }
        return contextMode.trim().toUpperCase(Locale.ROOT);
    }
}
