package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardCommentDto;
import com.childcare.domain.board.dto.BoardCommentRequest;
import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardComment;
import com.childcare.domain.board.entity.BoardItem;
import com.childcare.domain.board.mapper.BoardMapper;
import com.childcare.domain.board.repository.BoardCommentLikeRepository;
import com.childcare.domain.board.repository.BoardCommentRepository;
import com.childcare.domain.board.repository.BoardItemRepository;
import com.childcare.domain.board.repository.BoardRepository;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class BoardCommentServicePocTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final BoardItemRepository boardItemRepository = mock(BoardItemRepository.class);
    private final BoardCommentRepository boardCommentRepository = mock(BoardCommentRepository.class);
    private final BoardCommentLikeRepository boardCommentLikeRepository = mock(BoardCommentLikeRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ForbiddenWordChecker forbiddenWordChecker = mock(ForbiddenWordChecker.class);
    private final BoardMapper boardMapper = mock(BoardMapper.class);

    private final BoardCommentService service = new BoardCommentService(
            boardRepository,
            boardItemRepository,
            boardCommentRepository,
            boardCommentLikeRepository,
            memberRepository,
            forbiddenWordChecker,
            boardMapper
    );

    @Test
    void commentList_blocksNeighborMismatchOnCommunityBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = neighborBoard(1L);
        BoardItem item = boardItem(11L, 1L, "R2", 20000, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class, () -> service.getComments(viewerId, 1L, 11L));
        assertEquals(BoardErrorCode.NEIGHBOR_ACCESS_DENIED.getCode(), ex.getCode());
        verifyNoInteractions(boardMapper);
        verify(boardCommentRepository, never()).findById(1L);
    }

    @Test
    void createComment_blocksNeighborMismatchOnCommunityBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = neighborBoard(1L);
        BoardItem item = boardItem(11L, 1L, "R2", 20000, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");
        BoardCommentRequest request = BoardCommentRequest.builder()
                .content("안녕")
                .secretYn("N")
                .fixYn("N")
                .build();

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class,
                () -> service.createComment(viewerId, 1L, 11L, request));
        assertEquals(BoardErrorCode.NEIGHBOR_ACCESS_DENIED.getCode(), ex.getCode());
        verify(boardCommentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createComment_rejectsParentCommentFromDifferentItem() {
        UUID viewerId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(11L, 1L, "R1", 11111, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");
        BoardCommentRequest request = BoardCommentRequest.builder()
                .content("안녕")
                .secretYn("N")
                .fixYn("N")
                .parentSeq(99L)
                .build();

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardCommentRepository.findById(99L)).thenReturn(Optional.of(boardComment(99L, 88L, item.getRegId(), "부모")));

        BoardException ex = assertThrows(BoardException.class,
                () -> service.createComment(viewerId, 1L, 11L, request));
        assertEquals(BoardErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void likeComment_rejectsCommentNotInTargetItem() {
        UUID viewerId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(11L, 1L, "R1", 11111, UUID.randomUUID());
        BoardComment wrongItemComment = boardComment(111L, 99L, item.getRegId(), "작성자");
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(boardCommentRepository.findById(111L)).thenReturn(Optional.of(wrongItemComment));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardCommentLikeRepository.existsByBcSeqAndMbId(111L, viewerId)).thenReturn(false);

        BoardException ex = assertThrows(BoardException.class,
                () -> service.likeComment(viewerId, 1L, 11L, 111L));
        assertEquals(BoardErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void commentBySlug_resolvesBoardByCodeWhenSlugNotFound() {
        UUID viewerId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(11L, 1L, "R1", 11111, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.empty());
        when(boardRepository.findByBoCode("COMMUNITY")).thenReturn(Optional.of(board));
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.getComments(11L, viewerId)).thenReturn(Collections.emptyList());

        var response = service.getCommentsBySlug(viewerId, "community", 11L);
        List<BoardCommentDto> data = response.getData();
        assertEquals(Collections.emptyList(), data);
    }

    private static Board neighborBoard(Long seq) {
        return Board.builder()
                .boSeq(seq)
                .boCode("COMMUNITY")
                .boSlug("community")
                .boTitle("커뮤니티")
                .boUseYn("Y")
                .boReadAuth("USER")
                .boWriteAuth("USER")
                .boDeleteAuth("USER")
                .boNeighborYn("Y")
                .build();
    }

    private static Board activeBoard(Long seq) {
        Board board = neighborBoard(seq);
        board.setBoNeighborYn("N");
        return board;
    }

    private static BoardItem boardItem(Long id, Long boardId, String regionCode, Integer postcode, UUID authorId) {
        return BoardItem.builder()
                .biSeq(id)
                .boSeq(boardId)
                .title("테스트 글")
                .content("내용")
                .biCategory("GENERAL")
                .readCount(0)
                .likeCount(0)
                .regUserRegionCode(regionCode)
                .regUserPostcode(postcode)
                .regId(authorId)
                .regDate(LocalDateTime.now())
                .build();
    }

    private static Member member(UUID id, Role role, String postcode, String regionCode) {
        return Member.builder()
                .id(id)
                .name("테스터")
                .phone("010-0000-0000")
                .email("test@example.com")
                .postcode(postcode)
                .regionCode(regionCode)
                .role(role)
                .build();
    }

    private static BoardComment boardComment(Long id, Long itemId, UUID authorId, String content) {
        return BoardComment.builder()
                .bcSeq(id)
                .biSeq(itemId)
                .parentSeq(null)
                .depth(0)
                .content(content)
                .likeCount(0)
                .secretYn("N")
                .fixYn(null)
                .regId(authorId)
                .regDate(LocalDateTime.now())
                .build();
    }
}
