package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class MemberException extends RuntimeException {

    private final String code;

    public MemberException(MemberErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum MemberErrorCode {
        CHILD_EXISTS("MEMBER_001", "등록된 자녀가 있어 탈퇴할 수 없습니다. 자녀를 먼저 삭제해주세요.");

        private final String code;
        private final String message;

        MemberErrorCode(String code, String message) {
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
