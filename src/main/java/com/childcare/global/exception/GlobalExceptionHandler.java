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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        log.error("Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(401).body(ApiResponse.error("AUTH_001", e.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        log.error("Auth error: {} - {}", e.getCode(), e.getMessage());
        // 토큰 관련 에러는 401 반환
        if ("AUTH_008".equals(e.getCode()) || "AUTH_009".equals(e.getCode())) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ChildAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleChildAccessDeniedException(ChildAccessDeniedException e) {
        log.error("Access denied: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(403).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ChildException.class)
    public ResponseEntity<ApiResponse<Void>> handleChildException(ChildException e) {
        log.error("Child error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(DiaryException.class)
    public ResponseEntity<ApiResponse<Void>> handleDiaryException(DiaryException e) {
        log.error("Diary error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CalendarException.class)
    public ResponseEntity<ApiResponse<Void>> handleCalendarException(CalendarException e) {
        log.error("Calendar error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(MemberException e) {
        log.error("Member error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<ApiResponse<Void>> handleBoardException(BoardException e) {
        log.error("Board error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ChildImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleChildImageException(ChildImageException e) {
        log.error("Child image error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ChecklistException.class)
    public ResponseEntity<ApiResponse<Void>> handleChecklistException(ChecklistException e) {
        log.error("Checklist error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(GrowthHistoryException.class)
    public ResponseEntity<ApiResponse<Void>> handleGrowthHistoryException(GrowthHistoryException e) {
        log.error("Growth history error: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getCode(), e.getMessage()));
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
