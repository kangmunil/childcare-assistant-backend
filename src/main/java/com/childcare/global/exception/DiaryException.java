package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class DiaryException extends RuntimeException {

    private final String code;

    public DiaryException(DiaryErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum DiaryErrorCode {
        ITEM_REQUIRED("DIARY_001", "항목은 필수 입력값입니다."),
        DATE_REQUIRED("DIARY_002", "날짜는 필수 입력값입니다."),
        TIME_REQUIRED("DIARY_003", "시간은 필수 입력값입니다."),
        ITEM_NOT_FOUND("DIARY_004", "존재하지 않는 항목입니다."),
        NOT_FOUND("DIARY_005", "성장일지를 찾을 수 없습니다.");

        private final String code;
        private final String message;

        DiaryErrorCode(String code, String message) {
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
