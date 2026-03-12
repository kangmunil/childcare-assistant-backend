package com.childcare.domain.board.service;

import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardFile;
import com.childcare.domain.board.entity.BoardItem;
import com.childcare.domain.board.repository.BoardFileRepository;
import com.childcare.domain.board.repository.BoardItemRepository;
import com.childcare.domain.board.repository.BoardRepository;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.entity.Role;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.childcare.global.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BoardFileServicePocTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final BoardItemRepository boardItemRepository = mock(BoardItemRepository.class);
    private final BoardFileRepository boardFileRepository = mock(BoardFileRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SupabaseStorageService storageService = mock(SupabaseStorageService.class);
    private final BoardImageOptimizationService boardImageOptimizationService = mock(BoardImageOptimizationService.class);

    private final BoardFileService service = new BoardFileService(
            boardRepository,
            boardItemRepository,
            boardFileRepository,
            memberRepository,
            storageService,
            boardImageOptimizationService
    );

    @Test
    void filesList_blocksNeighborMismatchOnCommunityBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = neighborBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R2", 22222, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class, () -> service.getFiles(viewerId, 1L, 10L));
        assertEquals(BoardErrorCode.NEIGHBOR_ACCESS_DENIED.getCode(), ex.getCode());
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadFiles_blocksNeighborMismatchOnCommunityBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = neighborBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R2", 22222, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class,
                () -> service.uploadFiles(viewerId, 1L, 10L, List.of(multipartFile)));
        assertEquals(BoardErrorCode.NEIGHBOR_ACCESS_DENIED.getCode(), ex.getCode());
        verifyNoInteractions(storageService);
    }

    @Test
    void downloadUrl_blocksItemNotInBoard() {
        UUID viewerId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(10L, 99L, "R1", 11111, UUID.randomUUID());
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        BoardException ex = assertThrows(BoardException.class, () -> service.getDownloadUrl(viewerId, 1L, 10L, 3L));
        assertEquals(BoardErrorCode.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void uploadFiles_rejectsDisallowedExtensionOnCommunityBoard() {
        UUID authorId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R1", 11111, authorId);
        Member author = member(authorId, Role.USER, "11111", "R1");
        MultipartFile multipartFile = mock(MultipartFile.class);

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(boardImageOptimizationService.isCommunityBoard(board)).thenReturn(true);
        when(boardImageOptimizationService.isCommunityUploadImageExtensionAllowed("svg")).thenReturn(false);
        when(boardFileRepository.findByBiSeq(10L)).thenReturn(List.of());
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("bad.svg");
        when(multipartFile.getContentType()).thenReturn("image/svg+xml");

        BoardException ex = assertThrows(BoardException.class,
                () -> service.uploadFiles(authorId, 1L, 10L, List.of(multipartFile)));
        assertEquals(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void uploadFiles_rejectsMimeMismatchOnCommunityBoard() {
        UUID authorId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R1", 11111, authorId);
        Member author = member(authorId, Role.USER, "11111", "R1");
        MultipartFile multipartFile = mock(MultipartFile.class);

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(boardImageOptimizationService.isCommunityBoard(board)).thenReturn(true);
        when(boardImageOptimizationService.isCommunityUploadImageExtensionAllowed("jpg")).thenReturn(true);
        when(boardImageOptimizationService.isCommunityUploadImageMimeAllowed("application/pdf")).thenReturn(false);
        when(boardFileRepository.findByBiSeq(10L)).thenReturn(List.of());
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("ok.jpg");
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        BoardException ex = assertThrows(BoardException.class,
                () -> service.uploadFiles(authorId, 1L, 10L, List.of(multipartFile)));
        assertEquals(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void uploadFiles_allowsJpgOnCommunityBoard() {
        UUID authorId = UUID.randomUUID();
        Board board = activeBoard(1L);
        BoardItem item = boardItem(10L, 1L, "R1", 11111, authorId);
        Member author = member(authorId, Role.USER, "11111", "R1");
        MultipartFile multipartFile = mock(MultipartFile.class);
        BoardFile saved = BoardFile.builder()
                .bfSeq(99L)
                .biSeq(10L)
                .orgFilename("ok.jpg")
                .bfName("saved.jpg")
                .bfPath("board/COMMUNITY/20260226/saved.jpg")
                .bfExtension("jpg")
                .bfSize(1024)
                .regId(authorId)
                .regDate(LocalDateTime.now())
                .build();

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(boardImageOptimizationService.isCommunityBoard(board)).thenReturn(true);
        when(boardImageOptimizationService.isCommunityUploadImageExtensionAllowed("jpg")).thenReturn(true);
        when(boardImageOptimizationService.isCommunityUploadImageMimeAllowed("image/jpeg")).thenReturn(true);
        when(boardFileRepository.findByBiSeq(10L)).thenReturn(List.of());
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("ok.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(boardFileRepository.save(any(BoardFile.class))).thenReturn(saved);
        when(storageService.getSignedUrl(anyString(), anyInt())).thenReturn("https://example.com/signed.jpg");

        var response = service.uploadFiles(authorId, 1L, 10L, List.of(multipartFile));
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
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

    private static BoardItem boardItem(Long id, Long boardId, String regionCode, Integer postcode, UUID authorId) {
        return BoardItem.builder()
                .biSeq(id)
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
                .phone("010-2222-2222")
                .email("tester2@example.com")
                .postcode(postcode)
                .regionCode(regionCode)
                .role(role)
                .build();
    }
}
