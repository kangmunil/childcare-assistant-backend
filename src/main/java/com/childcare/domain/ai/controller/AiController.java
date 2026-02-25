package com.childcare.domain.ai.controller;

import com.childcare.domain.ai.dto.AiChatRequest;
import com.childcare.domain.ai.dto.AiChatResponse;
import com.childcare.domain.ai.service.AiService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.AiException;
import com.childcare.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody AiChatRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage() == null ? "잘못된 요청입니다." : error.getDefaultMessage())
                    .orElse("잘못된 요청입니다.");
            throw new AiException(AiException.AiErrorCode.BAD_REQUEST, message);
        }

        String traceId = StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
        UUID memberId = SecurityUtil.getCurrentMemberId();
        request.setUserId(memberId.toString());

        int messageLength = request.getMessage() == null ? 0 : request.getMessage().length();
        log.info("Chat request received. requestId={}, memberId={}, childId={}, contextMode={}, intentHint={}, requestedProfileDomains={}, sessionId={}, messageLength={}, hasManualContext={}, hasGrowthContext={}",
                traceId,
                memberId,
                request.getChildId(),
                request.getEffectiveContextMode(),
                request.getIntentHint(),
                request.getRequestedProfileDomains(),
                request.getSessionId(),
                messageLength,
                StringUtils.hasText(request.getProfileContext()),
                request.getGrowthContext() != null && !request.getGrowthContext().isEmpty());

        AiChatResponse response = aiService.chat(request, traceId, memberId);
        return ResponseEntity.ok()
                .header("X-Request-Id", traceId)
                .body(ApiResponse.success(response));
    }
}
