package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.*;
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
public class BoardItemService {

    private final BoardRepository boardRepository;
    private final BoardItemRepository boardItemRepository;
    private final BoardFileRepository boardFileRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final BoardItemReadRepository boardItemReadRepository;
    private final BoardItemLikeRepository boardItemLikeRepository;
    private final MemberRepository memberRepository;
    private final ForbiddenWordChecker forbiddenWordChecker;
    private final BoardMapper boardMapper;

    /**
     * 게시글 목록 조회
     */
    public ApiResponse<Map<String, Object>> getItemList(UUID memberId, Long boardId, BoardSearchRequest searchRequest) {
        log.info("Get item list for board: {}, member: {}", boardId, memberId);
        Board board = validateBoard(boardId);
        return getItemListInternal(memberId, board, searchRequest);
    }

    /**
     * 게시글 목록 조회 (slug 기반)
     */
    public ApiResponse<Map<String, Object>> getItemListBySlug(UUID memberId, String slug,
            BoardSearchRequest searchRequest) {
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return getItemListInternal(memberId, board, searchRequest);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public ApiResponse<BoardItemDto> getItem(UUID memberId, Long boardId, Long itemId) {
        log.info("Get item: {} for board: {}, member: {}", itemId, boardId, memberId);
        Board board = validateBoard(boardId);
        return getItemInternal(memberId, board, itemId);
    }

    /**
     * 게시글 상세 조회 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardItemDto> getItemBySlug(UUID memberId, String slug, Long itemId) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Board board = validateBoardBySlug(normalizedSlug);
        log.info("Get item: {} for board slug: {}, member: {}", itemId, normalizedSlug, memberId);

        return getItemInternal(memberId, board, itemId);
    }

    /**
     * 게시글 작성
     */
    @Transactional
    public ApiResponse<BoardItemDto> createItem(UUID memberId, Long boardId, BoardItemRequest request) {
        log.info("Create item for board: {}, member: {}", boardId, memberId);

        // 게시판 조회 및 검증
        Board board = validateBoard(boardId);
        return createItemInternal(memberId, board, request);
    }

    /**
     * 게시글 작성 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardItemDto> createItemBySlug(UUID memberId, String slug, BoardItemRequest request) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Board board = validateBoardBySlug(normalizedSlug);
        log.info("Create item for board slug: {}, member: {}", normalizedSlug, memberId);

        return createItemInternal(memberId, board, request);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public ApiResponse<BoardItemDto> updateItem(UUID memberId, Long boardId, Long itemId, BoardItemRequest request) {
        log.info("Update item: {} for board: {}, member: {}", itemId, boardId, memberId);

        // 게시판 조회 및 검증
        Board board = validateBoard(boardId);
        return updateItemInternal(memberId, board, itemId, request);
    }

    /**
     * 게시글 수정 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardItemDto> updateItemBySlug(UUID memberId, String slug, Long itemId,
            BoardItemRequest request) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Board board = validateBoardBySlug(normalizedSlug);
        log.info("Update item: {} for board slug: {}, member: {}", itemId, normalizedSlug, memberId);

        return updateItemInternal(memberId, board, itemId, request);
    }

    private ApiResponse<BoardItemDto> updateItemInternal(UUID memberId, Board board, Long itemId,
            BoardItemRequest request) {
        BoardItem item = validateItem(itemId);

        // 수정 권한 검증
        Member member = getMember(memberId);
        validateModifyPermission(board, member, item.getRegId());

        // 필수값 검증
        validateItemRequest(request);

        // 금지어 검사
        if (forbiddenWordChecker.containsForbiddenWord(request.getTitle(), request.getContent())) {
            throw new BoardException(BoardErrorCode.FORBIDDEN_WORD_DETECTED);
        }

        // 고정 여부 (ADMIN만 설정 가능)
        if ("ADMIN".equals(member.getRole().name())) {
            item.setFixYn("Y".equals(request.getFixYn()) ? "Y" : null);
        }

        // 게시글 수정
        item.setTitle(request.getTitle());
        item.setContent(request.getContent());
        item.setBiCategory(normalizeCategory(request.getCategory()));
        item.setUpdateId(memberId);
        item.setUpdateDate(LocalDateTime.now());

        BoardItem savedItem = boardItemRepository.save(item);

        // 첨부파일 조회
        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);
        long commentCount = boardCommentRepository.countByBiSeqAndDeleteYnIsNull(itemId);
        boolean liked = boardItemLikeRepository.existsByBiSeqAndMbId(itemId, memberId);

        return ApiResponse.success("게시글 수정 성공", toDto(savedItem, files, (int) commentCount, liked, memberId));
    }

    /**
     * 게시글 삭제 (소프트 삭제)
     */
    @Transactional
    public ApiResponse<Void> deleteItem(UUID memberId, Long boardId, Long itemId) {
        log.info("Delete item: {} for board: {}, member: {}", itemId, boardId, memberId);

        // 게시판 조회 및 검증
        Board board = validateBoard(boardId);
        return deleteItemInternal(memberId, board, itemId);
    }

    /**
     * 게시글 삭제 (slug 기반)
     */
    @Transactional
    public ApiResponse<Void> deleteItemBySlug(UUID memberId, String slug, Long itemId) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Board board = validateBoardBySlug(normalizedSlug);
        log.info("Delete item: {} for board slug: {}, member: {}", itemId, normalizedSlug, memberId);

        return deleteItemInternal(memberId, board, itemId);
    }

    private ApiResponse<Void> deleteItemInternal(UUID memberId, Board board, Long itemId) {
        BoardItem item = validateItem(itemId);

        // 삭제 권한 검증
        Member member = getMember(memberId);
        validateDeletePermission(board, member, item.getRegId());

        // 첨부파일 하드 삭제
        boardFileRepository.deleteByBiSeq(itemId);

        // 게시글 소프트 삭제
        item.setDeleteYn("Y");
        item.setDeleteId(memberId);
        item.setDeleteDate(LocalDateTime.now());

        boardItemRepository.save(item);

        return ApiResponse.success("게시글 삭제 성공", null);
    }

    /**
     * 게시글 공감
     */
    @Transactional
    public ApiResponse<Integer> likeItem(UUID memberId, Long boardId, Long itemId) {
        log.info("Like item: {} for member: {}", itemId, memberId);

        // 게시판 및 게시글 조회
        validateBoard(boardId);
        return likeItemInternal(memberId, boardId, itemId);
    }

    /**
     * 게시글 공감 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> likeItemBySlug(UUID memberId, String slug, Long itemId) {
        log.info("Like item: {} for board slug: {}, member: {}", itemId, slug, memberId);
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return likeItemInternal(memberId, board.getBoSeq(), itemId);
    }

    /**
     * 게시글 공감 취소
     */
    @Transactional
    public ApiResponse<Integer> unlikeItem(UUID memberId, Long boardId, Long itemId) {
        log.info("Unlike item: {} for member: {}", itemId, memberId);

        // 게시판 및 게시글 조회
        validateBoard(boardId);
        return unlikeItemInternal(memberId, boardId, itemId);
    }

    /**
     * 게시글 공감 취소 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> unlikeItemBySlug(UUID memberId, String slug, Long itemId) {
        log.info("Unlike item: {} for board slug: {}, member: {}", itemId, slug, memberId);
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        return unlikeItemInternal(memberId, board.getBoSeq(), itemId);
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
                .orElseGet(() -> boardRepository.findByBoCode(slug.toUpperCase(Locale.ROOT))
                        .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND)));

        if (!"Y".equals(board.getBoUseYn())) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_AVAILABLE);
        }
        return board;
    }

    private ApiResponse<BoardItemDto> createItemInternal(UUID memberId, Board board, BoardItemRequest request) {
        // 작성 권한 검증
        Member member = getMember(memberId);
        validateWritePermission(board, member);

        // 필수값 검증
        validateItemRequest(request);

        // 금지어 검사
        if (forbiddenWordChecker.containsForbiddenWord(request.getTitle(), request.getContent())) {
            throw new BoardException(BoardErrorCode.FORBIDDEN_WORD_DETECTED);
        }

        // 동네 게시판인 경우 우편번호 검증
        Integer userPostcode = null;
        if ("Y".equals(board.getBoNeighborYn())) {
            userPostcode = validateNeighborAuth(member);
        }

        // 고정 여부 (ADMIN만 설정 가능)
        String fixYn = null;
        if ("Y".equals(request.getFixYn()) && "ADMIN".equals(member.getRole().name())) {
            fixYn = "Y";
        }

        // 게시글 저장
        BoardItem item = BoardItem.builder()
                .boSeq(board.getBoSeq())
                .title(request.getTitle())
                .content(request.getContent())
                .biCategory(normalizeCategory(request.getCategory()))
                .readCount(0)
                .likeCount(0)
                .fixYn(fixYn)
                .regUserPostcode(userPostcode)
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        BoardItem savedItem = boardItemRepository.save(item);

        BoardItemDto dto = toDto(savedItem, Collections.emptyList(), 0, false, memberId);

        return ApiResponse.success("게시글 작성 성공", dto);
    }

    private BoardItem validateItem(Long itemId) {
        BoardItem item = boardItemRepository.findById(itemId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.ITEM_NOT_FOUND));

        if ("Y".equals(item.getDeleteYn())) {
            throw new BoardException(BoardErrorCode.ITEM_ALREADY_DELETED);
        }
        return item;
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
        // ADMIN은 항상 수정 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // USER 권한인 경우 작성자만 수정 가능
        if (!member.getId().equals(authorId)) {
            throw new BoardException(BoardErrorCode.MODIFY_PERMISSION_DENIED);
        }
    }

    private void validateDeletePermission(Board board, Member member, UUID authorId) {
        // ADMIN 권한 게시판이면 ADMIN만 삭제 가능
        if ("ADMIN".equals(board.getBoDeleteAuth())) {
            if (!"ADMIN".equals(member.getRole().name())) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        } else {
            // USER 권한인 경우 작성자 또는 ADMIN만 삭제 가능
            if (!"ADMIN".equals(member.getRole().name()) && !member.getId().equals(authorId)) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        }
    }

    private Integer validateNeighborAuth(Member member) {
        if (member.getPostcode() == null || member.getPostcode().isBlank()) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }
        try {
            return Integer.parseInt(member.getPostcode().substring(0, 3));
        } catch (Exception e) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }
    }

    private void validateItemRequest(BoardItemRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BoardException(BoardErrorCode.ITEM_TITLE_REQUIRED);
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BoardException(BoardErrorCode.ITEM_CONTENT_REQUIRED);
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BoardException(BoardErrorCode.ITEM_CATEGORY_INVALID);
        }
    }

    private void increaseReadCount(Long itemId, UUID memberId) {
        if (!boardItemReadRepository.existsByBiSeqAndMbId(itemId, memberId)) {
            BoardItemRead read = BoardItemRead.builder()
                    .biSeq(itemId)
                    .mbId(memberId)
                    .regDate(LocalDateTime.now())
                    .build();
            boardItemReadRepository.save(read);

            BoardItem item = boardItemRepository.findById(itemId).orElse(null);
            if (item != null) {
                item.setReadCount((item.getReadCount() == null ? 0 : item.getReadCount()) + 1);
                boardItemRepository.save(item);
            }
        }
    }

    private ApiResponse<Integer> likeItemInternal(UUID memberId, Long boardId, Long itemId) {
        BoardItem item = validateItem(itemId);
        if (!item.getBoSeq().equals(boardId)) {
            throw new BoardException(BoardErrorCode.ITEM_NOT_FOUND);
        }

        // 이미 공감했는지 확인
        if (boardItemLikeRepository.existsByBiSeqAndMbId(itemId, memberId)) {
            throw new BoardException(BoardErrorCode.ALREADY_LIKED);
        }

        // 공감 저장
        BoardItemLike like = BoardItemLike.builder()
                .biSeq(itemId)
                .mbId(memberId)
                .regDate(LocalDateTime.now())
                .build();
        boardItemLikeRepository.save(like);

        // 공감수 증가
        int newLikeCount = (item.getLikeCount() == null ? 0 : item.getLikeCount()) + 1;
        item.setLikeCount(newLikeCount);
        boardItemRepository.save(item);

        return ApiResponse.success("공감 성공", newLikeCount);
    }

    private ApiResponse<Integer> unlikeItemInternal(UUID memberId, Long boardId, Long itemId) {
        BoardItem item = validateItem(itemId);
        if (!item.getBoSeq().equals(boardId)) {
            throw new BoardException(BoardErrorCode.ITEM_NOT_FOUND);
        }

        // 공감했는지 확인
        BoardItemLike like = boardItemLikeRepository.findByBiSeqAndMbId(itemId, memberId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.NOT_LIKED));

        // 공감 삭제
        boardItemLikeRepository.delete(like);

        // 공감수 감소
        int newLikeCount = Math.max(0, (item.getLikeCount() == null ? 0 : item.getLikeCount()) - 1);
        item.setLikeCount(newLikeCount);
        boardItemRepository.save(item);

        return ApiResponse.success("공감 취소 성공", newLikeCount);
    }

    private ApiResponse<BoardItemDto> getItemInternal(UUID memberId, Board board, Long itemId) {
        BoardItem item = validateItem(itemId);

        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);

        // 동네 게시판인 경우 동네 검증
        if ("Y".equals(board.getBoNeighborYn())) {
            Integer userPostcode = validateNeighborAuth(member);
            if (!userPostcode.equals(item.getRegUserPostcode())) {
                throw new BoardException(BoardErrorCode.NEIGHBOR_ACCESS_DENIED);
            }
        }

        // 조회수 증가 (중복 방지)
        increaseReadCount(itemId, memberId);

        // 첨부파일 조회
        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);

        // 댓글 수 조회
        long commentCount = boardCommentRepository.countByBiSeqAndDeleteYnIsNull(itemId);

        // 공감 여부
        boolean liked = boardItemLikeRepository.existsByBiSeqAndMbId(itemId, memberId);

        // DTO 변환
        BoardItemDto dto = toDto(item, files, (int) commentCount, liked, memberId);

        return ApiResponse.success("게시글 조회 성공", dto);
    }

    private ApiResponse<Map<String, Object>> getItemListInternal(UUID memberId, Board board,
            BoardSearchRequest searchRequest) {
        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);

        // 동네 게시판인 경우 우편번호 검증
        Integer userPostcode = null;
        if ("Y".equals(board.getBoNeighborYn())) {
            userPostcode = validateNeighborAuth(member);
        }

        String category = normalizeCategory(searchRequest.getCategory());
        Long boardId = board.getBoSeq();

        // 고정글 조회
        List<BoardItemListDto> fixedDtos = boardMapper.getFixedItems(boardId, category, memberId);
        Set<Long> fixedIds = fixedDtos.stream().map(BoardItemListDto::getId).collect(Collectors.toSet());

        // 인기글 조회 (조회수+공감수 상위 3건, 고정글 제외)
        List<BoardItemListDto> popularDtos = boardMapper.getPopularItems(boardId, category, memberId);
        popularDtos = popularDtos.stream()
                .filter(item -> !fixedIds.contains(item.getId()))
                .peek(item -> item.setPopular(true))
                .collect(Collectors.toList());

        // 일반글 조회
        Map<String, Object> searchResult = searchItems(boardId, userPostcode, searchRequest, category, memberId);
        @SuppressWarnings("unchecked")
        List<BoardItemListDto> normalDtos = (List<BoardItemListDto>) searchResult.get("items");

        Map<String, Object> result = new HashMap<>();
        result.put("fixedItems", fixedDtos);
        result.put("popularItems", popularDtos);
        result.put("items", normalDtos);
        result.put("totalElements", searchResult.get("totalElements"));
        result.put("totalPages", searchResult.get("totalPages"));
        result.put("currentPage", searchResult.get("currentPage"));
        result.put("size", searchResult.get("size"));

        return ApiResponse.success("게시글 목록 조회 성공", result);
    }

    private Map<String, Object> searchItems(Long boardId, Integer userPostcode, BoardSearchRequest searchRequest,
            String category, UUID memberId) {
        String keyword = searchRequest.getKeyword();
        String searchType = searchRequest.getSearchType();
        int page = searchRequest.getPage();
        int size = searchRequest.getSize();
        int offset = page * size;

        // searchType이 없으면 기본값 titleContent
        if (searchType == null || searchType.isBlank()) {
            searchType = "titleContent";
        }

        // Mapper로 검색
        List<BoardItemListDto> items = boardMapper.searchItems(boardId, userPostcode, category, memberId, searchType,
                keyword, offset, size);
        int totalCount = boardMapper.countSearchItems(boardId, userPostcode, category, searchType, keyword);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("totalElements", totalCount);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    private BoardItemDto toDto(BoardItem item, List<BoardFile> files, int commentCount, boolean liked, UUID memberId) {
        String authorName = memberRepository.findById(item.getRegId())
                .map(Member::getName)
                .orElse("Unknown");

        String boardTitle = boardRepository.findById(item.getBoSeq())
                .map(Board::getBoTitle)
                .orElse("");

        List<BoardFileDto> fileDtos = files.stream()
                .map(file -> BoardFileDto.builder()
                        .id(file.getBfSeq())
                        .itemId(file.getBiSeq())
                        .orgFilename(file.getOrgFilename())
                        .fileName(file.getBfName())
                        .filePath(file.getBfPath())
                        .extension(file.getBfExtension())
                        .fileSize(file.getBfSize())
                        .regDate(file.getRegDate())
                        .build())
                .collect(Collectors.toList());

        return BoardItemDto.builder()
                .id(item.getBiSeq())
                .boardId(item.getBoSeq())
                .boardTitle(boardTitle)
                .title(item.getTitle())
                .content(item.getContent())
                .category(item.getBiCategory())
                .readCount(item.getReadCount())
                .likeCount(item.getLikeCount())
                .fixYn(item.getFixYn())
                .regUserPostcode(item.getRegUserPostcode())
                .regId(item.getRegId())
                .regUserName(authorName)
                .regDate(item.getRegDate())
                .updateId(item.getUpdateId())
                .updateDate(item.getUpdateDate())
                .files(fileDtos)
                .commentCount(commentCount)
                .liked(liked)
                .isAuthor(item.getRegId().equals(memberId))
                .build();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("qna", "daily", "tip").contains(normalized)) {
            throw new BoardException(BoardErrorCode.ITEM_CATEGORY_INVALID);
        }
        return normalized;
    }
}
