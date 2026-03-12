package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class ChildImageException extends RuntimeException {

    private final String code;

    public ChildImageException(ChildImageErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum ChildImageErrorCode {
        EMPTY_FILE("CHILDIMAGE_001", "업로드할 파일이 비어있습니다."),
        INVALID_IMAGE_TYPE("CHILDIMAGE_002", "허용되지 않는 이미지 형식입니다. (허용: jpg, jpeg, png, gif, bmp, webp, svg)"),
        IMAGE_NOT_FOUND("CHILDIMAGE_003", "삭제할 이미지가 없습니다."),
        IMAGE_UPLOAD_FAILED("CHILDIMAGE_004", "이미지 업로드에 실패했습니다.");

        private final String code;
        private final String message;

        ChildImageErrorCode(String code, String message) {
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
