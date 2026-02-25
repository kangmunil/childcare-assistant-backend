package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardCommentDto;
import com.childcare.domain.board.dto.BoardCommentListDto;
import com.childcare.domain.board.dto.BoardCommentRequest;
import com.childcare.domain.board.entity.*;
import com.childcare.domain.board.mapper.BoardMapper;
import com.childcare.domain.board.repository.*;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardRepository boardRepository;
    private final BoardItemRepository boardItemRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final BoardCommentLikeRepository boardCommentLikeRepository;
    private final MemberRepository memberRepository;
    private final ForbiddenWordChecker forbiddenWordChecker;
    private final BoardMapper boardMapper;

    /**
     * 댓글 목록 조회
     * - DB에서 정렬된 상태로 조회 (고정댓글 > 부모-자식 그룹핑 > 등록일순)
     * - depth는 1까지만 지원 (댓글 + 대댓글)
     */
    public ApiResponse<List<BoardCommentDto>> getComments(UUID memberId, Long boardId, Long itemId) {
        log.info("Get comments for item: {}, member: {}", itemId, memberId);

        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);

        // MyBatis로 댓글 목록 조회 (작성자명, 공감여부 포함)
        List<BoardCommentListDto> comments = boardMapper.getComments(itemId, memberId);

        // 플랫 리스트로 반환 (프론트에서 그룹핑)
        List<BoardCommentDto> result = comments.stream()
                .map(comment -> toDto(comment, memberId, item.getRegId()))
                .collect(Collectors.toList());

        return ApiResponse.success("댓글 목록 조회 성공", result);
    }

    /**
     * 댓글 목록 조회 (slug 기반)
     */
    public ApiResponse<List<BoardCommentDto>> getCommentsBySlug(UUID memberId, String slug, Long itemId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return getComments(memberId, board.getBoSeq(), itemId);
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public ApiResponse<BoardCommentDto> createComment(UUID memberId, Long boardId, Long itemId, BoardCommentRequest request) {
        log.info("Create comment for item: {}, member: {}", itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 작성 권한 검증
        Member member = getMember(memberId);
        validateWritePermission(board, member);

        // 필수값 검증
        validateCommentRequest(request);

        // 금지어 검사
        if (forbiddenWordChecker.containsForbiddenWord(request.getContent())) {
            throw new BoardException(BoardErrorCode.FORBIDDEN_WORD_DETECTED);
        }

        // 대댓글인 경우
        Integer depth = 0;
        Long parentSeq = null;
        if (request.getParentSeq() != null) {
            BoardComment parentComment = boardCommentRepository.findById(request.getParentSeq())
                    .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));

            // depth 제한 (대댓글까지만 허용, depth > 0인 댓글에는 답글 불가)
            if (parentComment.getDepth() != null && parentComment.getDepth() > 0) {
                throw new BoardException(BoardErrorCode.REPLY_DEPTH_EXCEEDED);
            }

            // 비밀 댓글에 대댓글 작성 권한 검증
            if ("Y".equals(parentComment.getSecretYn())) {
                if (!canAccessSecretComment(memberId, item.getRegId(), parentComment.getRegId())) {
                    throw new BoardException(BoardErrorCode.SECRET_COMMENT_REPLY_DENIED);
                }
            }

            parentSeq = request.getParentSeq();
            depth = 1;  // 대댓글은 항상 depth=1
        }

        // 고정 여부 (ADMIN만 설정 가능, 대댓글은 고정 불가)
        String fixYn = null;
        if ("Y".equals(request.getFixYn()) && "ADMIN".equals(member.getRole().name()) && parentSeq == null) {
            fixYn = "Y";
        }

        // 댓글 저장
        BoardComment comment = BoardComment.builder()
                .biSeq(itemId)
                .parentSeq(parentSeq)
                .depth(depth)
                .content(request.getContent())
                .likeCount(0)
                .secretYn(request.getSecretYn())
                .fixYn(fixYn)
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        BoardComment savedComment = boardCommentRepository.save(comment);

        BoardCommentDto dto = toDto(savedComment, memberId, item.getRegId(), Collections.emptyList());

        return ApiResponse.success("댓글 작성 성공", dto);
    }

    /**
     * 댓글 작성 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardCommentDto> createCommentBySlug(UUID memberId, String slug, Long itemId, BoardCommentRequest request) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return createComment(memberId, board.getBoSeq(), itemId, request);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public ApiResponse<BoardCommentDto> updateComment(UUID memberId, Long boardId, Long itemId, Long commentId, BoardCommentRequest request) {
        log.info("Update comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        // 수정 권한 검증
        Member member = getMember(memberId);
        validateModifyPermission(board, member, comment.getRegId());

        // 필수값 검증
        validateCommentRequest(request);

        // 금지어 검사
        if (forbiddenWordChecker.containsForbiddenWord(request.getContent())) {
            throw new BoardException(BoardErrorCode.FORBIDDEN_WORD_DETECTED);
        }

        // 고정 여부 (ADMIN만 설정 가능)
        if ("ADMIN".equals(member.getRole().name()) && comment.getParentSeq() == null) {
            comment.setFixYn("Y".equals(request.getFixYn()) ? "Y" : null);
        }

        // 댓글 수정
        comment.setContent(request.getContent());
        comment.setSecretYn(request.getSecretYn());
        comment.setUpdateId(memberId);
        comment.setUpdateDate(LocalDateTime.now());

        BoardComment savedComment = boardCommentRepository.save(comment);

        BoardCommentDto dto = toDto(savedComment, memberId, item.getRegId(), Collections.emptyList());

        return ApiResponse.success("댓글 수정 성공", dto);
    }

    /**
     * 댓글 수정 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardCommentDto> updateCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId, BoardCommentRequest request) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return updateComment(memberId, board.getBoSeq(), itemId, commentId, request);
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    @Transactional
    public ApiResponse<Void> deleteComment(UUID memberId, Long boardId, Long itemId, Long commentId) {
        log.info("Delete comment: {} for item: {}, member: {}", commentId, itemId, memberId);

        // 게시판 및 댓글 조회
        Board board = validateBoard(boardId);
        validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        // 삭제 권한 검증
        Member member = getMember(memberId);
        validateDeletePermission(board, member, comment.getRegId());

        // 댓글 소프트 삭제
        comment.setDeleteYn("Y");
        comment.setDeleteId(memberId);
        comment.setDeleteDate(LocalDateTime.now());

        boardCommentRepository.save(comment);

        return ApiResponse.success("댓글 삭제 성공", null);
    }

    /**
     * 댓글 삭제 (slug 기반)
     */
    @Transactional
    public ApiResponse<Void> deleteCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return deleteComment(memberId, board.getBoSeq(), itemId, commentId);
    }

    /**
     * 댓글 공감
     */
    @Transactional
    public ApiResponse<Integer> likeComment(UUID memberId, Long boardId, Long itemId, Long commentId) {
        log.info("Like comment: {} for member: {}", commentId, memberId);

        // 게시판 및 댓글 조회
        validateBoard(boardId);
        validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        // 이미 공감했는지 확인
        if (boardCommentLikeRepository.existsByBcSeqAndMbId(commentId, memberId)) {
            throw new BoardException(BoardErrorCode.ALREADY_LIKED);
        }

        // 공감 저장
        BoardCommentLike like = BoardCommentLike.builder()
                .bcSeq(commentId)
                .mbId(memberId)
                .regDate(LocalDateTime.now())
                .build();
        boardCommentLikeRepository.save(like);

        // 공감수 증가
        int newLikeCount = (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1;
        comment.setLikeCount(newLikeCount);
        boardCommentRepository.save(comment);

        return ApiResponse.success("공감 성공", newLikeCount);
    }

    /**
     * 댓글 공감 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> likeCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return likeComment(memberId, board.getBoSeq(), itemId, commentId);
    }

    /**
     * 댓글 공감 취소
     */
    @Transactional
    public ApiResponse<Integer> unlikeComment(UUID memberId, Long boardId, Long itemId, Long commentId) {
        log.info("Unlike comment: {} for member: {}", commentId, memberId);

        // 게시판 및 댓글 조회
        validateBoard(boardId);
        validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        // 공감했는지 확인
        BoardCommentLike like = boardCommentLikeRepository.findByBcSeqAndMbId(commentId, memberId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.NOT_LIKED));

        // 공감 삭제
        boardCommentLikeRepository.delete(like);

        // 공감수 감소
        int newLikeCount = Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1);
        comment.setLikeCount(newLikeCount);
        boardCommentRepository.save(comment);

        return ApiResponse.success("공감 취소 성공", newLikeCount);
    }

    /**
     * 댓글 공감 취소 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> unlikeCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return unlikeComment(memberId, board.getBoSeq(), itemId, commentId);
    }

    /**
     * 댓글 고정
     */
    @Transactional
    public ApiResponse<Void> pinComment(UUID memberId, Long boardId, Long itemId, Long commentId) {
        Board board = validateBoard(boardId);
        Member member = getMember(memberId);
        validateWritePermission(board, member);
        validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        comment.setFixYn("Y");
        boardCommentRepository.save(comment);

        return ApiResponse.success("댓글 고정 성공", null);
    }

    /**
     * 댓글 고정 (slug 기반)
     */
    @Transactional
    public ApiResponse<Void> pinCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return pinComment(memberId, board.getBoSeq(), itemId, commentId);
    }

    /**
     * 댓글 고정 해제
     */
    @Transactional
    public ApiResponse<Void> unpinComment(UUID memberId, Long boardId, Long itemId, Long commentId) {
        Board board = validateBoard(boardId);
        Member member = getMember(memberId);
        validateWritePermission(board, member);
        validateItemInBoard(boardId, itemId);
        BoardComment comment = validateComment(commentId);

        comment.setFixYn(null);
        boardCommentRepository.save(comment);

        return ApiResponse.success("댓글 고정 해제 성공", null);
    }

    /**
     * 댓글 고정 해제 (slug 기반)
     */
    @Transactional
    public ApiResponse<Void> unpinCommentBySlug(UUID memberId, String slug, Long itemId, Long commentId) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return unpinComment(memberId, board.getBoSeq(), itemId, commentId);
    }

    // ========== Private Methods ==========

    private Board validateBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));

        if (!"Y".equals(board.getBoUseYn())) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_AVAILABLE);
        }
        return board;
    }

    private Board validateBoardBySlug(String slug) {
        Board board = boardRepository.findByBoSlug(slug)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));

        if (!"Y".equals(board.getBoUseYn())) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_AVAILABLE);
        }
        return board;
    }

    private BoardItem validateItem(Long itemId) {
        BoardItem item = boardItemRepository.findById(itemId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.ITEM_NOT_FOUND));

        if ("Y".equals(item.getDeleteYn())) {
            throw new BoardException(BoardErrorCode.ITEM_ALREADY_DELETED);
        }
        return item;
    }

    private BoardItem validateItemInBoard(Long boardId, Long itemId) {
        BoardItem item = validateItem(itemId);
        if (!item.getBoSeq().equals(boardId)) {
            throw new BoardException(BoardErrorCode.ITEM_NOT_FOUND);
        }
        return item;
    }

    private BoardComment validateComment(Long commentId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));

        if ("Y".equals(comment.getDeleteYn())) {
            throw new BoardException(BoardErrorCode.COMMENT_ALREADY_DELETED);
        }
        return comment;
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.READ_PERMISSION_DENIED));
    }

    private void validateReadPermission(Board board, Member member) {
        if ("ADMIN".equals(board.getBoReadAuth()) && !"ADMIN".equals(member.getRole().name())) {
            throw new BoardException(BoardErrorCode.READ_PERMISSION_DENIED);
        }
    }

    private void validateWritePermission(Board board, Member member) {
        if ("ADMIN".equals(board.getBoWriteAuth()) && !"ADMIN".equals(member.getRole().name())) {
            throw new BoardException(BoardErrorCode.WRITE_PERMISSION_DENIED);
        }
    }

    private void validateModifyPermission(Board board, Member member, UUID authorId) {
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        if (!member.getId().equals(authorId)) {
            throw new BoardException(BoardErrorCode.MODIFY_PERMISSION_DENIED);
        }
    }

    private void validateDeletePermission(Board board, Member member, UUID authorId) {
        if ("ADMIN".equals(board.getBoDeleteAuth())) {
            if (!"ADMIN".equals(member.getRole().name())) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        } else {
            if (!"ADMIN".equals(member.getRole().name()) && !member.getId().equals(authorId)) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        }
    }

    private boolean canAccessSecretComment(UUID memberId, UUID itemAuthorId, UUID commentAuthorId) {
        return memberId.equals(itemAuthorId) || memberId.equals(commentAuthorId);
    }

    private void validateCommentRequest(BoardCommentRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BoardException(BoardErrorCode.COMMENT_CONTENT_REQUIRED);
        }
    }

    /**
     * 대댓글용 toDto (replies 없음) - BoardComment 엔티티용
     */
    private BoardCommentDto toDto(BoardComment comment, UUID memberId, UUID itemAuthorId) {
        return toDto(comment, memberId, itemAuthorId, Collections.emptyList());
    }

    /**
     * 부모 댓글용 toDto (replies 포함) - BoardComment 엔티티용
     */
    private BoardCommentDto toDto(BoardComment comment, UUID memberId, UUID itemAuthorId, List<BoardCommentDto> replies) {
        boolean isDeleted = "Y".equals(comment.getDeleteYn());
        boolean isSecret = "Y".equals(comment.getSecretYn());
        boolean canAccess = !isSecret || canAccessSecretComment(memberId, itemAuthorId, comment.getRegId());
        boolean liked = boardCommentLikeRepository.existsByBcSeqAndMbId(comment.getBcSeq(), memberId);

        String authorName = memberRepository.findById(comment.getRegId())
                .map(Member::getName)
                .orElse("Unknown");

        // 삭제된 댓글 또는 비밀 댓글 접근 불가 시 내용 숨김
        String content = comment.getContent();
        if (isDeleted) {
            content = "삭제된 댓글입니다.";
            authorName = "";
        } else if (!canAccess) {
            content = "비밀 댓글입니다.";
        }

        return BoardCommentDto.builder()
                .id(comment.getBcSeq())
                .itemId(comment.getBiSeq())
                .parentSeq(comment.getParentSeq())
                .depth(comment.getDepth())
                .content(content)
                .likeCount(comment.getLikeCount())
                .secretYn(comment.getSecretYn())
                .fixYn(comment.getFixYn())
                .deleteYn(comment.getDeleteYn())
                .regId(isDeleted ? null : comment.getRegId())
                .regUserName(authorName)
                .regDate(comment.getRegDate())
                .updateId(comment.getUpdateId())
                .updateDate(comment.getUpdateDate())
                .replies(replies)
                .liked(liked)
                .isAuthor(comment.getRegId().equals(memberId))
                .accessible(canAccess)
                .build();
    }

    /**
     * 대댓글용 toDto (replies 없음) - MyBatis DTO용
     */
    private BoardCommentDto toDto(BoardCommentListDto comment, UUID memberId, UUID itemAuthorId) {
        return toDto(comment, memberId, itemAuthorId, Collections.emptyList());
    }

    /**
     * 부모 댓글용 toDto (replies 포함) - MyBatis DTO용
     */
    private BoardCommentDto toDto(BoardCommentListDto comment, UUID memberId, UUID itemAuthorId, List<BoardCommentDto> replies) {
        boolean isDeleted = "Y".equals(comment.getDeleteYn());
        boolean isSecret = "Y".equals(comment.getSecretYn());
        boolean canAccess = !isSecret || canAccessSecretComment(memberId, itemAuthorId, comment.getRegId());

        String authorName = comment.getRegUserName() != null ? comment.getRegUserName() : "Unknown";

        // 삭제된 댓글 또는 비밀 댓글 접근 불가 시 내용 숨김
        String content = comment.getContent();
        if (isDeleted) {
            content = "삭제된 댓글입니다.";
            authorName = "";
        } else if (!canAccess) {
            content = "비밀 댓글입니다.";
        }

        return BoardCommentDto.builder()
                .id(comment.getId())
                .itemId(comment.getItemId())
                .parentSeq(comment.getParentSeq())
                .depth(comment.getDepth())
                .content(content)
                .likeCount(comment.getLikeCount())
                .secretYn(comment.getSecretYn())
                .fixYn(comment.getFixYn())
                .deleteYn(comment.getDeleteYn())
                .regId(isDeleted ? null : comment.getRegId())
                .regUserName(authorName)
                .regDate(comment.getRegDate())
                .updateId(comment.getUpdateId())
                .updateDate(comment.getUpdateDate())
                .replies(replies)
                .liked(comment.isLiked())
                .isAuthor(comment.getRegId().equals(memberId))
                .accessible(canAccess)
                .build();
    }
}
