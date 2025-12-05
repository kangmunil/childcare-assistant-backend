package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final String code;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum AuthErrorCode {
        ID_ALREADY_EXISTS("AUTH_002", "이미 사용 중인 아이디입니다."),
        EMAIL_ALREADY_EXISTS("AUTH_003", "이미 사용 중인 이메일입니다."),
        INVALID_REFERRAL_CODE("AUTH_004", "유효하지 않은 추천인 코드입니다."),
        INVALID_CREDENTIALS("AUTH_005", "아이디 또는 비밀번호가 일치하지 않습니다."),
        REGISTRATION_FAILED("AUTH_006", "회원가입에 실패했습니다."),
        INVITE_CODE_GENERATION_FAILED("AUTH_007", "초대코드 생성에 실패했습니다. 다시 시도해주세요.");

        private final String code;
        private final String message;

        AuthErrorCode(String code, String message) {
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
