package com.childcare.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AiException(AiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus();
    }

    public AiException(AiErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus();
    }

    public enum AiErrorCode {
        TIMEOUT("AI_001_TIMEOUT", "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", HttpStatus.GATEWAY_TIMEOUT),
        UPSTREAM_ERROR("AI_002_UPSTREAM", "AI 서비스에서 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_GATEWAY),
        BAD_REQUEST("AI_003_BAD_REQUEST", "잘못된 AI 요청입니다.", HttpStatus.BAD_REQUEST),
        UNAVAILABLE("AI_004_UNAVAILABLE", "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE);

        private final String code;
        private final String message;
        private final HttpStatus status;

        AiErrorCode(String code, String message, HttpStatus status) {
            this.code = code;
            this.message = message;
            this.status = status;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
