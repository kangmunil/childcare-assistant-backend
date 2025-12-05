package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class ChildException extends RuntimeException {

    private final String code;

    public ChildException(ChildErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum ChildErrorCode {
        NAME_REQUIRED("CHILD_001", "이름은 필수 입력값입니다."),
        BIRTHDAY_REQUIRED("CHILD_002", "생년월일은 필수 입력값입니다."),
        BIRTHTIME_REQUIRED("CHILD_003", "태어난 시각은 필수 입력값입니다."),
        NOT_FOUND("CHILD_004", "자녀 정보를 찾을 수 없습니다."),
        ALREADY_DELETED("CHILD_005", "이미 삭제된 자녀 정보입니다.");

        private final String code;
        private final String message;

        ChildErrorCode(String code, String message) {
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
