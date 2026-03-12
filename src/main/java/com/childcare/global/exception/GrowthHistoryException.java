package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class GrowthHistoryException extends RuntimeException {

    private final String code;

    public GrowthHistoryException(GrowthHistoryErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum GrowthHistoryErrorCode {
        HEIGHT_REQUIRED("GROWTH_001", "키는 필수 입력값입니다."),
        WEIGHT_REQUIRED("GROWTH_002", "몸무게는 필수 입력값입니다."),
        NOT_FOUND("GROWTH_003", "성장 이력을 찾을 수 없습니다.");

        private final String code;
        private final String message;

        GrowthHistoryErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
