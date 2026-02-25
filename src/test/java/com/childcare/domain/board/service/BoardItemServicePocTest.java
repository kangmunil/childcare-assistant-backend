package com.childcare.domain.board.service;

import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardItem;
import com.childcare.domain.board.mapper.BoardMapper;
import com.childcare.domain.board.repository.BoardCommentRepository;
import com.childcare.domain.board.repository.BoardItemLikeRepository;
import com.childcare.domain.board.repository.BoardItemReadRepository;
import com.childcare.domain.board.repository.BoardItemRepository;
import com.childcare.domain.board.repository.BoardRepository;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.childcare.domain.board.repository.BoardFileRepository;
import com.childcare.global.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardItemServicePocTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final BoardItemRepository boardItemRepository = mock(BoardItemRepository.class);
    private final BoardFileRepository boardFileRepository = mock(BoardFileRepository.class);
    private final BoardCommentRepository boardCommentRepository = mock(BoardCommentRepository.class);
    private final BoardItemReadRepository boardItemReadRepository = mock(BoardItemReadRepository.class);
    private final BoardItemLikeRepository boardItemLikeRepository = mock(BoardItemLikeRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ForbiddenWordChecker forbiddenWordChecker = mock(ForbiddenWordChecker.class);
    private final BoardMapper boardMapper = mock(BoardMapper.class);
    private final SupabaseStorageService storageService = mock(SupabaseStorageService.class);

    private final BoardItemService service = new BoardItemService(
            boardRepository,
            boardItemRepository,
            boardFileRepository,
            mock(com.childcare.domain.board.repository.BoardCommentRepository.class),
            boardItemReadRepository,
            boardItemLikeRepository,
            memberRepository,
            forbiddenWordChecker,
            boardMapper,
            storageService
    );

    @Test
    void likeItem_blocksNeighborMismatchOnCommunityBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = neighborBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R1", 11111, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "22222", "R2");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class, () -> service.likeItem(viewerId, 1L, 10L));
        assertEquals(BoardErrorCode.NEIGHBOR_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void likeItem_blocksItemNotInBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem itemInAnotherBoard = boardItem(10L, 99L, "R1", 11111, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(itemInAnotherBoard));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class, () -> service.likeItem(viewerId, 1L, 10L));
        assertEquals(BoardErrorCode.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }

    private static Board neighborBoard(Long seq) {
        return Board.builder()
                .boSeq(seq)
                .boCode("COMMUNITY")
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

    private static BoardItem boardItem(Long itemId, Long boardId, String regionCode, Integer postcode, UUID authorId) {
        return BoardItem.builder()
                .biSeq(itemId)
                .boSeq(boardId)
                .title("테스트 글")
                .content("테스트")
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
                .phone("010-1111-1111")
                .email("tester@example.com")
                .postcode(postcode)
                .regionCode(regionCode)
                .role(role)
                .build();
    }
}
