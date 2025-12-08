package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class ChildAccessDeniedException extends RuntimeException {

    private final AccessErrorCode errorCode;

    public ChildAccessDeniedException() {
        super(AccessErrorCode.NO_ACCESS.getMessage());
        this.errorCode = AccessErrorCode.NO_ACCESS;
    }

    public ChildAccessDeniedException(AccessErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ChildAccessDeniedException(String message) {
        super(message);
        this.errorCode = AccessErrorCode.NO_ACCESS;
    }

    public String getCode() {
        return errorCode.getCode();
    }

    @Getter
    public enum AccessErrorCode {
        NO_ACCESS("ACCESS_001", "해당 자녀에 대한 접근 권한이 없습니다."),
        NO_READ_PERMISSION("ACCESS_003", "조회 권한이 없습니다."),
        NO_WRITE_PERMISSION("ACCESS_004", "등록/수정 권한이 없습니다."),
        NO_DELETE_PERMISSION("ACCESS_005", "삭제 권한이 없습니다."),
        NO_MANAGE_PERMISSION("ACCESS_006", "관리 권한이 없습니다.");

        private final String code;
        private final String message;

        AccessErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
