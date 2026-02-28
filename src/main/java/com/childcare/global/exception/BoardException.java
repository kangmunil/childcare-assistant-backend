package com.childcare.global.exception;

import lombok.Getter;

@Getter
public class BoardException extends RuntimeException {

    private final String code;

    public BoardException(BoardErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public enum BoardErrorCode {
        // 게시판 관련
        BOARD_NOT_FOUND("BOARD_001", "게시판을 찾을 수 없습니다."),
        BOARD_NOT_AVAILABLE("BOARD_002", "사용할 수 없는 게시판입니다."),
        BOARD_SLUG_RESERVED("BOARD_029", "사용할 수 없는 게시판 식별자입니다."),

        // 게시글 관련
        ITEM_NOT_FOUND("BOARD_003", "게시글을 찾을 수 없습니다."),
        ITEM_ALREADY_DELETED("BOARD_004", "이미 삭제된 게시글입니다."),
        ITEM_TITLE_REQUIRED("BOARD_026", "제목은 필수 입력값입니다."),
        ITEM_CONTENT_REQUIRED("BOARD_027", "내용은 필수 입력값입니다."),
        ITEM_CATEGORY_INVALID("BOARD_030", "유효하지 않은 카테고리입니다."),
        ITEM_PLACE_REQUIRED("BOARD_031", "동네후기 게시글에는 장소 선택이 필요합니다."),
        ITEM_SCOPE_CATEGORY_INVALID("BOARD_032", "게시 범위에 맞지 않는 카테고리입니다."),
        ITEM_URGENT_RESOLVE_INVALID("BOARD_033", "긴급/SOS 게시글만 해결 처리할 수 있습니다."),

        // 댓글 관련
        COMMENT_NOT_FOUND("BOARD_005", "댓글을 찾을 수 없습니다."),
        COMMENT_ALREADY_DELETED("BOARD_006", "이미 삭제된 댓글입니다."),
        SECRET_COMMENT_ACCESS_DENIED("BOARD_007", "비밀 댓글에 접근할 수 없습니다."),
        SECRET_COMMENT_REPLY_DENIED("BOARD_008", "비밀 댓글에 대댓글을 작성할 수 없습니다."),
        REPLY_DEPTH_EXCEEDED("BOARD_025", "대댓글에는 답글을 작성할 수 없습니다."),
        COMMENT_CONTENT_REQUIRED("BOARD_028", "댓글 내용은 필수 입력값입니다."),

        // 권한 관련
        READ_PERMISSION_DENIED("BOARD_009", "읽기 권한이 없습니다."),
        WRITE_PERMISSION_DENIED("BOARD_010", "작성 권한이 없습니다."),
        DELETE_PERMISSION_DENIED("BOARD_011", "삭제 권한이 없습니다."),
        MODIFY_PERMISSION_DENIED("BOARD_012", "수정 권한이 없습니다."),

        // 동네 게시판 관련
        NEIGHBOR_AUTH_REQUIRED("BOARD_013", "동네 인증이 필요합니다. 주소를 등록해주세요."),
        NEIGHBOR_ACCESS_DENIED("BOARD_014", "해당 동네의 게시글만 조회할 수 있습니다."),

        // 파일 관련
        FILE_NOT_FOUND("BOARD_015", "파일을 찾을 수 없습니다."),
        FILE_COUNT_EXCEEDED("BOARD_016", "첨부파일 개수 제한을 초과했습니다."),
        FILE_SIZE_EXCEEDED("BOARD_017", "첨부파일 용량 제한을 초과했습니다."),
        FILE_EXTENSION_NOT_ALLOWED("BOARD_018", "허용되지 않는 파일 확장자입니다."),
        FILE_UPLOAD_DENIED("BOARD_022", "파일 업로드 권한이 없습니다."),
        FILE_DELETE_DENIED("BOARD_023", "파일 삭제 권한이 없습니다."),
        FILE_UPLOAD_FAILED("BOARD_024", "파일 업로드에 실패했습니다."),

        // 공감 관련
        ALREADY_LIKED("BOARD_019", "이미 공감한 게시글/댓글입니다."),
        NOT_LIKED("BOARD_020", "공감하지 않은 게시글/댓글입니다."),

        // 금지어 관련
        FORBIDDEN_WORD_DETECTED("BOARD_021", "금지어가 포함되어 있습니다.");

        private final String code;
        private final String message;

        BoardErrorCode(String code, String message) {
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
