package com.childcare.global.exception;

public class ChildAccessDeniedException extends RuntimeException {

    public ChildAccessDeniedException() {
        super("해당 자녀에 대한 권한이 없습니다.");
    }

    public ChildAccessDeniedException(String message) {
        super(message);
    }
}
