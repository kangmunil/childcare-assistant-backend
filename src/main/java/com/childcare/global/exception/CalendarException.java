package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class CalendarException extends RuntimeException {

    private final String code;

    public CalendarException(CalendarErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum CalendarErrorCode {
        TITLE_REQUIRED("CALENDAR_001", "일정명은 필수 입력값입니다."),
        DATE_REQUIRED("CALENDAR_002", "날짜는 필수 입력값입니다."),
        TIME_REQUIRED("CALENDAR_003", "시간은 필수 입력값입니다."),
        NOT_FOUND("CALENDAR_004", "일정을 찾을 수 없습니다.");

        private final String code;
        private final String message;

        CalendarErrorCode(String code, String message) {
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
