package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class ChecklistException extends RuntimeException {

    private final String code;

    public ChecklistException(ChecklistErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum ChecklistErrorCode {
        ITEM_REQUIRED("CHECKLIST_001", "체크리스트 항목은 필수 입력값입니다."),
        ITEM_NOT_FOUND("CHECKLIST_002", "존재하지 않는 체크리스트 항목입니다."),
        ALREADY_CHECKED("CHECKLIST_003", "이미 체크된 항목입니다."),
        NOT_CHECKED("CHECKLIST_004", "체크되지 않은 항목입니다.");

        private final String code;
        private final String message;

        ChecklistErrorCode(String code, String message) {
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
