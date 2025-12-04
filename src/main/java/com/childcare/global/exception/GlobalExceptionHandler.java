package com.childcare.global.exception;

import com.childcare.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ChildAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleChildAccessDeniedException(ChildAccessDeniedException e) {
        log.error("Access denied: {}", e.getMessage());
        return ResponseEntity.status(403).body(ApiResponse.error("ACCESS_001", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("Request body parsing error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("REQUEST_001", "요청 데이터를 읽을 수 없습니다. 형식을 확인해주세요."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_001", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error occurred: ", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error("SERVER_001", "예기치 못한 오류가 발생했습니다."));
    }
}
