package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.dto.BoardItemRequest;
import com.childcare.domain.board.dto.BoardItemListDto;
import com.childcare.domain.board.dto.BoardItemDto;
import com.childcare.domain.board.dto.BoardUrgentResolveRequest;
import com.childcare.domain.board.dto.BoardSearchRequest;
import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardFile;
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
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.childcare.domain.board.repository.BoardFileRepository;
import com.childcare.global.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private final BoardImageOptimizationService boardImageOptimizationService = mock(BoardImageOptimizationService.class);

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
            storageService,
            boardImageOptimizationService
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

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_populatesOptimizedThumbnailVariantFields() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1");

        BoardItemListDto listItem = BoardItemListDto.builder()
                .id(100L)
                .title("썸네일 테스트")
                .content("content")
                .hasFile(true)
                .regUserRegionName("서울시 강남구 역삼동")
                .build();
        BoardFile file = BoardFile.builder()
                .bfSeq(500L)
                .biSeq(100L)
                .bfPath("board/COMMUNITY/20260228/master.jpg")
                .bfExtension("jpg")
                .build();
        BoardFileDto.ImageVariantSet thumbVariant = BoardFileDto.ImageVariantSet.builder()
                .avifUrl("https://example.com/thumb.avif")
                .webpUrl("https://example.com/thumb.webp")
                .jpegUrl("https://example.com/thumb.jpeg")
                .pngUrl(null)
                .width(320)
                .height(240)
                .build();
        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(listItem));
        when(boardMapper.countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(boardFileRepository.findByBiSeqInOrderByRegDateAsc(List.of(100L))).thenReturn(List.of(file));
        when(boardImageOptimizationService.getPreferredThumbnailUrlsByFileIds(any()))
                .thenReturn(Map.of(500L, "https://example.com/thumb.webp"));
        when(boardImageOptimizationService.getPreferredThumbnailVariantSetsByFileIds(any()))
                .thenReturn(Map.of(500L, thumbVariant));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());

        BoardItemListDto item = items.get(0);
        assertEquals("https://example.com/thumb.webp", item.getThumbnailUrl());
        assertEquals("https://example.com/thumb.avif", item.getThumbnailAvifUrl());
        assertEquals("https://example.com/thumb.webp", item.getThumbnailWebpUrl());
        assertEquals("https://example.com/thumb.jpeg", item.getThumbnailJpegUrl());
        assertEquals(320, item.getThumbnailWidth());
        assertEquals(240, item.getThumbnailHeight());
        assertFalse(item.isSameNeighborhood());
        assertTrue(item.getRegUserRegionDongLabel() != null && !item.getRegUserRegionDongLabel().isBlank());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_fallsBackToMasterThumbnailWhenVariantMissing() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");

        BoardItemListDto listItem = BoardItemListDto.builder()
                .id(101L)
                .title("fallback")
                .content("content")
                .hasFile(true)
                .regUserRegionName("서울시 강남구 역삼동")
                .build();
        BoardFile file = BoardFile.builder()
                .bfSeq(501L)
                .biSeq(101L)
                .bfPath("board/COMMUNITY/20260228/master-fallback.jpg")
                .bfExtension("jpg")
                .build();
        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(listItem));
        when(boardMapper.countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(boardFileRepository.findByBiSeqInOrderByRegDateAsc(List.of(101L))).thenReturn(List.of(file));
        when(boardImageOptimizationService.getPreferredThumbnailUrlsByFileIds(any())).thenReturn(Map.of());
        when(boardImageOptimizationService.getPreferredThumbnailVariantSetsByFileIds(any())).thenReturn(Map.of());
        when(storageService.getSignedUrl(anyString(), anyInt())).thenReturn("https://example.com/master-fallback.jpg");

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());

        BoardItemListDto item = items.get(0);
        assertEquals("https://example.com/master-fallback.jpg", item.getThumbnailUrl());
        assertNull(item.getThumbnailAvifUrl());
        assertNull(item.getThumbnailWebpUrl());
        assertNull(item.getThumbnailJpegUrl());
        assertNull(item.getThumbnailPngUrl());
        assertNull(item.getThumbnailWidth());
        assertNull(item.getThumbnailHeight());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_usesPopularSortForAllScope() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("all")
                .sort("popular")
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of());
        when(boardMapper.countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        verify(boardMapper).searchItems(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                eq(viewerId),
                eq("titleContent"),
                any(),
                eq("all"),
                eq("popular"),
                eq(0),
                eq(20));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_forcesLatestSortForNeighborScope() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .sort("popular")
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of());
        when(boardMapper.countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        verify(boardMapper).searchItems(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                eq(viewerId),
                eq("titleContent"),
                any(),
                eq("neighbor"),
                eq("latest"),
                eq(0),
                eq(20));
    }

    @Test
    void createItemBySlug_allScopeAllowsMissingLocation() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "", "", null);
        BoardItemRequest request = BoardItemRequest.builder()
                .title("전체 글")
                .content("위치 없이 작성")
                .category("daily")
                .postScope("all")
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(boardItemRepository.save(any(BoardItem.class))).thenAnswer(invocation -> {
            BoardItem item = invocation.getArgument(0);
            item.setBiSeq(500L);
            return item;
        });

        ApiResponse<BoardItemDto> response = service.createItemBySlug(authorId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("all", response.getData().getPostScope());

        ArgumentCaptor<BoardItem> captor = ArgumentCaptor.forClass(BoardItem.class);
        verify(boardItemRepository).save(captor.capture());
        BoardItem saved = captor.getValue();
        assertEquals("all", saved.getLocationScope());
        assertNull(saved.getRegUserRegionCode());
        assertNull(saved.getRegUserPostcode());
        assertNull(saved.getRegUserRegionName());
    }

    @Test
    void createItemBySlug_neighborScopeWithoutLocationThrowsBoard013() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "", "", null);
        BoardItemRequest request = BoardItemRequest.builder()
                .title("동네 글")
                .content("위치 필요")
                .category("local_info")
                .postScope("neighbor")
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        BoardException ex = assertThrows(BoardException.class, () -> service.createItemBySlug(authorId, "community", request));
        assertEquals(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void createItemBySlug_localReviewWithoutPlaceThrowsBoard031() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardItemRequest request = BoardItemRequest.builder()
                .title("동네후기")
                .content("장소없음")
                .category("local_review")
                .postScope("neighbor")
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        BoardException ex = assertThrows(BoardException.class, () -> service.createItemBySlug(authorId, "community", request));
        assertEquals(BoardErrorCode.ITEM_PLACE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void createItemBySlug_scopeCategoryMismatchThrowsBoard032() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardItemRequest request = BoardItemRequest.builder()
                .title("범위 불일치")
                .content("카테고리 불일치")
                .category("local_review")
                .postScope("all")
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        BoardException ex = assertThrows(BoardException.class, () -> service.createItemBySlug(authorId, "community", request));
        assertEquals(BoardErrorCode.ITEM_SCOPE_CATEGORY_INVALID.getCode(), ex.getCode());
    }

    @Test
    void updateItemBySlug_scopeChangeRequestIsIgnored() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardItem existing = BoardItem.builder()
                .biSeq(10L)
                .boSeq(1L)
                .title("기존")
                .content("기존 내용")
                .biCategory("local_info")
                .locationScope("neighbor")
                .regId(authorId)
                .regDate(LocalDateTime.now())
                .build();
        BoardItemRequest request = BoardItemRequest.builder()
                .title("수정")
                .content("수정 내용")
                .category("info_share")
                .postScope("all")
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        BoardException ex = assertThrows(BoardException.class, () -> service.updateItemBySlug(authorId, "community", 10L, request));
        assertEquals(BoardErrorCode.ITEM_SCOPE_CATEGORY_INVALID.getCode(), ex.getCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_urgentSlotReturnsOnlyMostRecentItemWithinTwoHours() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        LocalDateTime now = LocalDateTime.now();

        BoardItemListDto recentTop = BoardItemListDto.builder()
                .id(900L)
                .title("최신 긴급")
                .category("urgent")
                .regDate(now.minusMinutes(10))
                .build();
        BoardItemListDto recentSecond = BoardItemListDto.builder()
                .id(901L)
                .title("두번째 긴급")
                .category("urgent")
                .regDate(now.minusMinutes(40))
                .build();
        BoardItemListDto oldUrgent = BoardItemListDto.builder()
                .id(902L)
                .title("오래된 긴급")
                .category("urgent")
                .regDate(now.minusHours(3))
                .build();

        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .category("urgent")
                .urgentSlot(true)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(recentTop, recentSecond, oldUrgent));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(900L, items.get(0).getId());
        assertEquals(1, response.getData().get("totalElements"));
        assertEquals(1, response.getData().get("totalPages"));
        assertEquals(0, response.getData().get("currentPage"));
        assertEquals(1, response.getData().get("size"));

        verify(boardMapper, never()).countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_urgentSlotReturnsEmptyWhenAllUrgentAreOld() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        LocalDateTime now = LocalDateTime.now();

        BoardItemListDto oldUrgentOne = BoardItemListDto.builder()
                .id(910L)
                .title("오래된 긴급1")
                .category("urgent")
                .regDate(now.minusHours(3))
                .build();
        BoardItemListDto oldUrgentTwo = BoardItemListDto.builder()
                .id(911L)
                .title("오래된 긴급2")
                .category("urgent")
                .regDate(now.minusHours(5))
                .build();

        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .category("urgent")
                .urgentSlot(true)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(oldUrgentOne, oldUrgentTwo));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());
        assertEquals(0, response.getData().get("totalElements"));
        assertEquals(0, response.getData().get("totalPages"));
        assertEquals(0, response.getData().get("currentPage"));
        assertEquals(1, response.getData().get("size"));

        verify(boardMapper, never()).countSearchItems(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_urgentSlotBoundaryIncludesPlusOneSecondExcludesMinusOneSecond() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        LocalDateTime now = LocalDateTime.now();

        BoardItemListDto boundaryIncluded = BoardItemListDto.builder()
                .id(920L)
                .title("경계 포함")
                .category("urgent")
                .regDate(now.minusHours(2).plusSeconds(1))
                .build();
        BoardItemListDto boundaryExcluded = BoardItemListDto.builder()
                .id(921L)
                .title("경계 제외")
                .category("urgent")
                .regDate(now.minusHours(2).minusSeconds(1))
                .build();

        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .category("urgent")
                .urgentSlot(true)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(boundaryIncluded, boundaryExcluded));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(920L, items.get(0).getId());
        assertEquals(1, response.getData().get("totalElements"));
        assertEquals(1, response.getData().get("size"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_urgentSlotWithMissingLocationReturnsEmptyWithoutFallback() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "", "", null);
        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .category("urgent")
                .urgentSlot(true)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());
        assertEquals(0, response.getData().get("totalElements"));
        assertEquals(0, response.getData().get("totalPages"));
        assertEquals(0, response.getData().get("currentPage"));
        assertEquals(1, response.getData().get("size"));

        verify(boardMapper, never()).searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void resolveUrgentItemBySlug_setsResolvedStateForUrgentPost() {
        UUID authorId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member author = member(authorId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        BoardItem urgentItem = BoardItem.builder()
                .biSeq(33L)
                .boSeq(1L)
                .title("긴급 요청")
                .content("도움 필요")
                .biCategory("urgent")
                .locationScope("neighbor")
                .urgentResolvedYn("N")
                .urgentResolvedDate(null)
                .regId(authorId)
                .regDate(LocalDateTime.now().minusMinutes(5))
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardItemRepository.findById(33L)).thenReturn(Optional.of(urgentItem));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(boardItemRepository.save(any(BoardItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(boardFileRepository.findByBiSeq(33L)).thenReturn(List.of());
        when(boardCommentRepository.countByBiSeqAndDeleteYnIsNull(33L)).thenReturn(0L);
        when(boardItemLikeRepository.existsByBiSeqAndMbId(33L, authorId)).thenReturn(false);

        ApiResponse<BoardItemDto> response = service.resolveUrgentItemBySlug(
                authorId,
                "community",
                33L,
                BoardUrgentResolveRequest.builder().resolved(true).build());

        assertNotNull(response);
        assertNotNull(response.getData());
        assertTrue(response.getData().isUrgentResolved());
        assertNotNull(response.getData().getUrgentResolvedDate());

        ArgumentCaptor<BoardItem> savedCaptor = ArgumentCaptor.forClass(BoardItem.class);
        verify(boardItemRepository).save(savedCaptor.capture());
        assertEquals("Y", savedCaptor.getValue().getUrgentResolvedYn());
        assertNotNull(savedCaptor.getValue().getUrgentResolvedDate());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getItemListBySlug_urgentSlotExcludesResolvedUrgentPost() {
        UUID viewerId = UUID.randomUUID();
        Board board = communityBoard(1L);
        Member viewer = member(viewerId, Role.USER, "11111", "R1", "서울시 강남구 역삼동");
        LocalDateTime now = LocalDateTime.now();

        BoardItemListDto resolvedRecent = BoardItemListDto.builder()
                .id(940L)
                .title("해결된 긴급")
                .category("urgent")
                .urgentResolved(true)
                .regDate(now.minusMinutes(10))
                .build();
        BoardItemListDto unresolvedRecent = BoardItemListDto.builder()
                .id(941L)
                .title("미해결 긴급")
                .category("urgent")
                .urgentResolved(false)
                .regDate(now.minusMinutes(20))
                .build();

        BoardSearchRequest request = BoardSearchRequest.builder()
                .includeHighlights(false)
                .locationScope("neighbor")
                .category("urgent")
                .urgentSlot(true)
                .page(0)
                .size(20)
                .build();

        when(boardRepository.findByBoSlug("community")).thenReturn(Optional.of(board));
        when(memberRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(boardMapper.searchItems(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(resolvedRecent, unresolvedRecent));

        ApiResponse<Map<String, Object>> response = service.getItemListBySlug(viewerId, "community", request);
        assertNotNull(response);
        assertNotNull(response.getData());

        List<BoardItemListDto> items = (List<BoardItemListDto>) response.getData().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(941L, items.get(0).getId());
        assertFalse(items.get(0).isUrgentResolved());
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

    private static Board communityBoard(Long seq) {
        Board board = activeBoard(seq);
        board.setBoSlug("community");
        board.setBoCode("COMMUNITY");
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
        return member(id, role, postcode, regionCode, null);
    }

    private static Member member(UUID id, Role role, String postcode, String regionCode, String regionName) {
        return Member.builder()
                .id(id)
                .name("테스터")
                .phone("010-1111-1111")
                .email("tester@example.com")
                .postcode(postcode)
                .regionName(regionName)
                .regionCode(regionCode)
                .role(role)
                .build();
    }
}
