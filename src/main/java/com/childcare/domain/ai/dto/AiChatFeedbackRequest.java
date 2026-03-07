package com.childcare.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatFeedbackRequest {

    @JsonProperty("session_id")
    @Size(max = 200, message = "session_id는 최대 200자까지 입력할 수 있습니다.")
    private String sessionId;

    @JsonProperty("request_id")
    @Size(max = 200, message = "request_id는 최대 200자까지 입력할 수 있습니다.")
    private String requestId;

    @NotBlank(message = "rating은 필수입니다.")
    @Pattern(regexp = "(?i)UP|DOWN", message = "rating은 UP 또는 DOWN이어야 합니다.")
    private String rating;

    @JsonProperty("reason_code")
    @Pattern(
            regexp = "(?i)(^$|INCORRECT|UNCLEAR|NOT_HELPFUL|OUTDATED|SAFETY_CONCERN|OTHER)",
            message = "reason_code가 올바르지 않습니다."
    )
    private String reasonCode;

    @JsonProperty("reason_detail")
    @Size(max = 300, message = "reason_detail은 최대 300자까지 입력할 수 있습니다.")
    private String reasonDetail;

    @JsonProperty("response_mode")
    @Pattern(regexp = "(?i)(^$|ANSWER|CLARIFY|FALLBACK)", message = "response_mode가 올바르지 않습니다.")
    private String responseMode;

    @Size(max = 50, message = "intent는 최대 50자까지 입력할 수 있습니다.")
    private String intent;

    @JsonProperty("is_first_ai_answer")
    private Boolean firstAiAnswer;
}
