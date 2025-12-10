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
    public ApiResponse<List<BoardCommentDto>> getComments(Long memberSeq, Long boardId, Long itemId) {
        log.info("Get comments for item: {}, member: {}", itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 읽기 권한 검증
        Member member = getMember(memberSeq);
        validateReadPermission(board, member);

        // MyBatis로 댓글 목록 조회 (작성자명, 공감여부 포함)
        List<BoardCommentListDto> comments = boardMapper.getComments(itemId, memberSeq);

        // 부모 댓글과 대댓글 그룹핑
        Map<Long, List<BoardCommentListDto>> repliesMap = comments.stream()
                .filter(c -> c.getParentSeq() != null)
                .collect(Collectors.groupingBy(BoardCommentListDto::getParentSeq));

        // 부모 댓글만 추출하여 DTO 변환 (대댓글은 replies에 포함)
        List<BoardCommentDto> result = comments.stream()
                .filter(c -> c.getParentSeq() == null)
                .map(parent -> {
                    List<BoardCommentDto> replies = repliesMap.getOrDefault(parent.getId(), Collections.emptyList())
                            .stream()
                            .map(reply -> toDto(reply, memberSeq, item.getRegUserSeq()))
                            .collect(Collectors.toList());
                    return toDto(parent, memberSeq, item.getRegUserSeq(), replies);
                })
                .collect(Collectors.toList());

        return ApiResponse.success("댓글 목록 조회 성공", result);
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public ApiResponse<BoardCommentDto> createComment(Long memberSeq, Long boardId, Long itemId, BoardCommentRequest request) {
        log.info("Create comment for item: {}, member: {}", itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 작성 권한 검증
        Member member = getMember(memberSeq);
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
                if (!canAccessSecretComment(memberSeq, item.getRegUserSeq(), parentComment.getRegUserSeq())) {
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
                .regUserSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .build();

        BoardComment savedComment = boardCommentRepository.save(comment);

        BoardCommentDto dto = toDto(savedComment, memberSeq, item.getRegUserSeq(), Collections.emptyList());

        return ApiResponse.success("댓글 작성 성공", dto);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public ApiResponse<BoardCommentDto> updateComment(Long memberSeq, Long boardId, Long itemId, Long commentId, BoardCommentRequest request) {
        log.info("Update comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);
        BoardComment comment = validateComment(commentId);

        // 수정 권한 검증
        Member member = getMember(memberSeq);
        validateModifyPermission(board, member, comment.getRegUserSeq());

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
        comment.setUpdateUserSeq(memberSeq);
        comment.setUpdateDate(LocalDateTime.now());

        BoardComment savedComment = boardCommentRepository.save(comment);

        BoardCommentDto dto = toDto(savedComment, memberSeq, item.getRegUserSeq(), Collections.emptyList());

        return ApiResponse.success("댓글 수정 성공", dto);
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    @Transactional
    public ApiResponse<Void> deleteComment(Long memberSeq, Long boardId, Long itemId, Long commentId) {
        log.info("Delete comment: {} for item: {}, member: {}", commentId, itemId, memberSeq);

        // 게시판 및 댓글 조회
        Board board = validateBoard(boardId);
        validateItem(itemId);
        BoardComment comment = validateComment(commentId);

        // 삭제 권한 검증
        Member member = getMember(memberSeq);
        validateDeletePermission(board, member, comment.getRegUserSeq());

        // 댓글 소프트 삭제
        comment.setDeleteYn("Y");
        comment.setDeleteUserSeq(String.valueOf(memberSeq));
        comment.setDeleteDate(LocalDateTime.now());

        boardCommentRepository.save(comment);

        return ApiResponse.success("댓글 삭제 성공", null);
    }

    /**
     * 댓글 공감
     */
    @Transactional
    public ApiResponse<Integer> likeComment(Long memberSeq, Long boardId, Long itemId, Long commentId) {
        log.info("Like comment: {} for member: {}", commentId, memberSeq);

        // 게시판 및 댓글 조회
        validateBoard(boardId);
        validateItem(itemId);
        BoardComment comment = validateComment(commentId);

        // 이미 공감했는지 확인
        if (boardCommentLikeRepository.existsByBcSeqAndMbSeq(commentId, memberSeq)) {
            throw new BoardException(BoardErrorCode.ALREADY_LIKED);
        }

        // 공감 저장
        BoardCommentLike like = BoardCommentLike.builder()
                .bcSeq(commentId)
                .mbSeq(memberSeq)
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
     * 댓글 공감 취소
     */
    @Transactional
    public ApiResponse<Integer> unlikeComment(Long memberSeq, Long boardId, Long itemId, Long commentId) {
        log.info("Unlike comment: {} for member: {}", commentId, memberSeq);

        // 게시판 및 댓글 조회
        validateBoard(boardId);
        validateItem(itemId);
        BoardComment comment = validateComment(commentId);

        // 공감했는지 확인
        BoardCommentLike like = boardCommentLikeRepository.findByBcSeqAndMbSeq(commentId, memberSeq)
                .orElseThrow(() -> new BoardException(BoardErrorCode.NOT_LIKED));

        // 공감 삭제
        boardCommentLikeRepository.delete(like);

        // 공감수 감소
        int newLikeCount = Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1);
        comment.setLikeCount(newLikeCount);
        boardCommentRepository.save(comment);

        return ApiResponse.success("공감 취소 성공", newLikeCount);
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

    private BoardItem validateItem(Long itemId) {
        BoardItem item = boardItemRepository.findById(itemId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.ITEM_NOT_FOUND));

        if ("Y".equals(item.getDeleteYn())) {
            throw new BoardException(BoardErrorCode.ITEM_ALREADY_DELETED);
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

    private Member getMember(Long memberSeq) {
        return memberRepository.findByMbSeq(memberSeq)
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

    private void validateModifyPermission(Board board, Member member, Long authorSeq) {
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        if (!member.getMbSeq().equals(authorSeq)) {
            throw new BoardException(BoardErrorCode.MODIFY_PERMISSION_DENIED);
        }
    }

    private void validateDeletePermission(Board board, Member member, Long authorSeq) {
        if ("ADMIN".equals(board.getBoDeleteAuth())) {
            if (!"ADMIN".equals(member.getRole().name())) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        } else {
            if (!"ADMIN".equals(member.getRole().name()) && !member.getMbSeq().equals(authorSeq)) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        }
    }

    private boolean canAccessSecretComment(Long memberSeq, Long itemAuthorSeq, Long commentAuthorSeq) {
        return memberSeq.equals(itemAuthorSeq) || memberSeq.equals(commentAuthorSeq);
    }

    private void validateCommentRequest(BoardCommentRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BoardException(BoardErrorCode.COMMENT_CONTENT_REQUIRED);
        }
    }

    /**
     * 대댓글용 toDto (replies 없음) - BoardComment 엔티티용
     */
    private BoardCommentDto toDto(BoardComment comment, Long memberSeq, Long itemAuthorSeq) {
        return toDto(comment, memberSeq, itemAuthorSeq, Collections.emptyList());
    }

    /**
     * 부모 댓글용 toDto (replies 포함) - BoardComment 엔티티용
     */
    private BoardCommentDto toDto(BoardComment comment, Long memberSeq, Long itemAuthorSeq, List<BoardCommentDto> replies) {
        boolean isDeleted = "Y".equals(comment.getDeleteYn());
        boolean isSecret = "Y".equals(comment.getSecretYn());
        boolean canAccess = !isSecret || canAccessSecretComment(memberSeq, itemAuthorSeq, comment.getRegUserSeq());
        boolean liked = boardCommentLikeRepository.existsByBcSeqAndMbSeq(comment.getBcSeq(), memberSeq);

        String authorName = memberRepository.findByMbSeq(comment.getRegUserSeq())
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
                .regUserSeq(isDeleted ? null : comment.getRegUserSeq())
                .regUserName(authorName)
                .regDate(comment.getRegDate())
                .updateUserSeq(comment.getUpdateUserSeq())
                .updateDate(comment.getUpdateDate())
                .replies(replies)
                .liked(liked)
                .isAuthor(comment.getRegUserSeq().equals(memberSeq))
                .accessible(canAccess)
                .deleted(isDeleted)
                .build();
    }

    /**
     * 대댓글용 toDto (replies 없음) - MyBatis DTO용
     */
    private BoardCommentDto toDto(BoardCommentListDto comment, Long memberSeq, Long itemAuthorSeq) {
        return toDto(comment, memberSeq, itemAuthorSeq, Collections.emptyList());
    }

    /**
     * 부모 댓글용 toDto (replies 포함) - MyBatis DTO용
     */
    private BoardCommentDto toDto(BoardCommentListDto comment, Long memberSeq, Long itemAuthorSeq, List<BoardCommentDto> replies) {
        boolean isDeleted = "Y".equals(comment.getDeleteYn());
        boolean isSecret = "Y".equals(comment.getSecretYn());
        boolean canAccess = !isSecret || canAccessSecretComment(memberSeq, itemAuthorSeq, comment.getRegUserSeq());

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
                .regUserSeq(isDeleted ? null : comment.getRegUserSeq())
                .regUserName(authorName)
                .regDate(comment.getRegDate())
                .updateUserSeq(comment.getUpdateUserSeq())
                .updateDate(comment.getUpdateDate())
                .replies(replies)
                .liked(comment.isLiked())
                .isAuthor(comment.getRegUserSeq().equals(memberSeq))
                .accessible(canAccess)
                .deleted(isDeleted)
                .build();
    }
}
