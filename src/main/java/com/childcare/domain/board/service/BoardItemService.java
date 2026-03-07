package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.*;
import com.childcare.domain.board.entity.*;
import com.childcare.domain.board.mapper.BoardMapper;
import com.childcare.domain.board.repository.*;
import com.childcare.domain.board.util.RegionLabelFormatter;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.childcare.global.service.SupabaseStorageService;
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
    private static final String COMMUNITY_BOARD_KEY = "community";
    private static final int SIGNED_URL_EXPIRE_SECONDS = 3600;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    private static final Set<String> COMMUNITY_ALL_CATEGORIES = Set.of(
            "tip", "qna", "item_review", "daily", "info_share");
    private static final Set<String> COMMUNITY_NEIGHBOR_CATEGORIES = Set.of(
            "urgent", "local_info", "local_review", "local_gathering", "local_share");
    private static final String LEGACY_COMPAT_HOSPITAL_CATEGORY = "hospital";
    private static final int COMMUNITY_URGENT_SLOT_WINDOW_HOURS = 2;
    private static final int COMMUNITY_URGENT_SLOT_MAX_ITEMS = 1;

    private final BoardRepository boardRepository;
    private final BoardItemRepository boardItemRepository;
    private final BoardFileRepository boardFileRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final BoardItemReadRepository boardItemReadRepository;
    private final BoardItemLikeRepository boardItemLikeRepository;
    private final MemberRepository memberRepository;
    private final ForbiddenWordChecker forbiddenWordChecker;
    private final BoardMapper boardMapper;
    private final SupabaseStorageService storageService;
    private final BoardImageOptimizationService boardImageOptimizationService;

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

    /**
     * 긴급/SOS 해결 처리 (boardId 기반)
     */
    @Transactional
    public ApiResponse<BoardItemDto> resolveUrgentItem(UUID memberId, Long boardId, Long itemId,
            BoardUrgentResolveRequest request) {
        Board board = validateBoard(boardId);
        return resolveUrgentItemInternal(memberId, board, itemId, request);
    }

    /**
     * 긴급/SOS 해결 처리 (slug 기반)
     */
    @Transactional
    public ApiResponse<BoardItemDto> resolveUrgentItemBySlug(UUID memberId, String slug, Long itemId,
            BoardUrgentResolveRequest request) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Board board = validateBoardBySlug(normalizedSlug);
        return resolveUrgentItemInternal(memberId, board, itemId, request);
    }

    private ApiResponse<BoardItemDto> resolveUrgentItemInternal(UUID memberId, Board board, Long itemId,
            BoardUrgentResolveRequest request) {
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);
        Member member = getMember(memberId);

        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
        }
        validateModifyPermission(board, member, item.getRegId());

        if (!isCommunityBoard(board) || !"urgent".equals(normalizeCategory(item.getBiCategory()))) {
            throw new BoardException(BoardErrorCode.ITEM_URGENT_RESOLVE_INVALID);
        }

        boolean nextResolved = request == null || request.isResolved();
        boolean currentResolved = isResolvedFlag(item.getUrgentResolvedYn());
        boolean changed = currentResolved != nextResolved;

        if (changed) {
            item.setUrgentResolvedYn(nextResolved ? "Y" : "N");
            item.setUrgentResolvedDate(nextResolved ? LocalDateTime.now() : null);
            item.setUpdateId(memberId);
            item.setUpdateDate(LocalDateTime.now());
            item = boardItemRepository.save(item);
        }

        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);
        long commentCount = boardCommentRepository.countByBiSeqAndDeleteYnIsNull(itemId);
        boolean liked = boardItemLikeRepository.existsByBiSeqAndMbId(itemId, memberId);

        String message = nextResolved ? "긴급/SOS 해결 처리 완료" : "긴급/SOS 해결 해제 완료";
        return ApiResponse.success(message, toDto(item, files, (int) commentCount, liked, memberId));
    }

    private ApiResponse<BoardItemDto> updateItemInternal(UUID memberId, Board board, Long itemId,
            BoardItemRequest request) {
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);

        // 수정 권한 검증
        Member member = getMember(memberId);
        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
        }
        validateModifyPermission(board, member, item.getRegId());

        // 필수값 검증
        validateItemRequest(request);
        validateCommunityUpdateRules(board, item, request);

        // 금지어 검사
        if (forbiddenWordChecker.containsForbiddenWord(request.getTitle(), request.getContent())) {
            throw new BoardException(BoardErrorCode.FORBIDDEN_WORD_DETECTED);
        }

        // 고정 여부 (ADMIN만 설정 가능)
        if ("ADMIN".equals(member.getRole().name())) {
            item.setFixYn("Y".equals(request.getFixYn()) ? "Y" : null);
        }

        // 게시글 수정
        String normalizedCategory = normalizeCategory(request.getCategory());
        item.setTitle(request.getTitle());
        item.setContent(request.getContent());
        item.setBiCategory(normalizedCategory);
        item.setPlaceName(request.getPlaceName());
        item.setPlaceAddress(request.getPlaceAddress());
        item.setPlaceLat(request.getPlaceLat());
        item.setPlaceLng(request.getPlaceLng());
        syncUrgentResolvedStateForCategory(item, normalizedCategory);
        // 1차 정책: 수정 시 게시글 공개 범위(locationScope/postScope) 변경은 지원하지 않음.
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
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);

        // 삭제 권한 검증
        Member member = getMember(memberId);
        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
        }
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
        Board board = validateBoard(boardId);
        Member member = getMember(memberId);
        return likeItemInternal(memberId, board, member, itemId);
    }

    /**
     * 게시글 공감 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> likeItemBySlug(UUID memberId, String slug, Long itemId) {
        log.info("Like item: {} for board slug: {}, member: {}", itemId, slug, memberId);
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        Member member = getMember(memberId);
        return likeItemInternal(memberId, board, member, itemId);
    }

    /**
     * 게시글 공감 취소
     */
    @Transactional
    public ApiResponse<Integer> unlikeItem(UUID memberId, Long boardId, Long itemId) {
        log.info("Unlike item: {} for member: {}", itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        Member member = getMember(memberId);
        return unlikeItemInternal(memberId, board, member, itemId);
    }

    /**
     * 게시글 공감 취소 (slug 기반)
     */
    @Transactional
    public ApiResponse<Integer> unlikeItemBySlug(UUID memberId, String slug, Long itemId) {
        log.info("Unlike item: {} for board slug: {}, member: {}", itemId, slug, memberId);
        Board board = validateBoardBySlug(slug.toLowerCase(Locale.ROOT));
        Member member = getMember(memberId);
        return unlikeItemInternal(memberId, board, member, itemId);
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

        boolean neighborBoard = isNeighborBoard(board);
        boolean communityBoard = isCommunityBoard(board);
        String postScope = normalizePostScope(resolveRequestedPostScope(request));
        String normalizedCategory = normalizeCategory(request.getCategory());
        if (communityBoard) {
            validateCommunityWriteRules(board, request, postScope);
        }

        // 동네 게시판/커뮤니티 글 작성 시 위치 정책 적용
        NeighborPostcodeScope postcodeScope = null;
        String regionNameSnapshot = RegionLabelFormatter.normalize(member.getRegionName());
        if (neighborBoard) {
            postcodeScope = resolveNeighborPostcodeScope(member);
            regionNameSnapshot = RegionLabelFormatter.normalize(member.getRegionName());
            postScope = "neighbor";
        } else if (communityBoard) {
            if (isNeighborPostScope(postScope)) {
                postcodeScope = validateCommunityNeighborPostPermission(member);
                regionNameSnapshot = RegionLabelFormatter.normalize(member.getRegionName());
            } else {
                postcodeScope = buildOptionalCommunityLocationScope(member);
                if (postcodeScope != null) {
                    regionNameSnapshot = RegionLabelFormatter.normalize(member.getRegionName());
                } else {
                    regionNameSnapshot = null;
                }
            }
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
                .biCategory(normalizedCategory)
                .placeName(request.getPlaceName())
                .placeAddress(request.getPlaceAddress())
                .placeLat(request.getPlaceLat())
                .placeLng(request.getPlaceLng())
                .urgentResolvedYn("N")
                .urgentResolvedDate(null)
                .readCount(0)
                .likeCount(0)
                .fixYn(fixYn)
                .locationScope(postScope)
                .regUserPostcode(postcodeScope == null ? null : postcodeScope.exactPostcode())
                .regUserRegionCode(postcodeScope == null ? null : postcodeScope.regionCode())
                .regUserRegionName(regionNameSnapshot)
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

    private BoardItem validateItemInBoard(Long boardId, Long itemId) {
        BoardItem item = validateItem(itemId);
        if (!item.getBoSeq().equals(boardId)) {
            throw new BoardException(BoardErrorCode.ITEM_NOT_FOUND);
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

    private void validateNeighborAccess(Board board, Member member, BoardItem item) {
        NeighborPostcodeScope scope = resolveNeighborPostcodeScope(member);
        if (!isNeighborMatched(item.getRegUserRegionCode(), item.getRegUserPostcode(), scope)) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_ACCESS_DENIED);
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

    private NeighborPostcodeScope resolveNeighborPostcodeScope(Member member) {
        String normalizedRegionCode = normalizeRegionCode(member.getRegionCode());
        String postcodeRaw = member.getPostcode();
        String digitsOnly = postcodeRaw == null ? "" : postcodeRaw.replaceAll("\\D", "");

        Integer exactPostcode = null;
        Integer legacyPrefix = null;

        if (!digitsOnly.isBlank()) {
            if (digitsOnly.length() < 3 && normalizedRegionCode == null) {
                throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
            }

            if (digitsOnly.length() >= 3) {
                try {
                    String exactRaw = digitsOnly.length() >= 5 ? digitsOnly.substring(0, 5) : digitsOnly;
                    String prefixRaw = digitsOnly.substring(0, 3);

                    exactPostcode = Integer.parseInt(exactRaw);
                    legacyPrefix = Integer.parseInt(prefixRaw);
                } catch (NumberFormatException e) {
                    if (normalizedRegionCode == null) {
                        throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
                    }
                    exactPostcode = null;
                    legacyPrefix = null;
                }
            }
        }

        if (normalizedRegionCode == null && exactPostcode == null) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }

        return new NeighborPostcodeScope(normalizedRegionCode, exactPostcode, legacyPrefix);
    }

    private boolean isNeighborMatched(String itemRegionCode, Integer itemPostcode, NeighborPostcodeScope scope) {
        if (scope == null) {
            return false;
        }

        String normalizedItemRegionCode = normalizeRegionCode(itemRegionCode);
        if (hasText(scope.regionCode()) && hasText(normalizedItemRegionCode)) {
            return scope.regionCode().equals(normalizedItemRegionCode);
        }

        if (scope.exactPostcode() == null || itemPostcode == null) {
            return false;
        }

        if (itemPostcode.equals(scope.exactPostcode())) {
            return true;
        }

        // Legacy data compatibility: historical posts may have 3-digit prefix only.
        return itemPostcode < 1000 && itemPostcode.equals(scope.legacyPrefix());
    }

    private String normalizeRegionCode(String regionCode) {
        if (regionCode == null) {
            return null;
        }
        String normalized = regionCode.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isNeighborBoard(Board board) {
        return board != null && "Y".equals(board.getBoNeighborYn());
    }

    private boolean isCommunityBoard(Board board) {
        if (board == null) {
            return false;
        }
        if (hasText(board.getBoSlug()) && COMMUNITY_BOARD_KEY.equalsIgnoreCase(board.getBoSlug().trim())) {
            return true;
        }
        return hasText(board.getBoCode()) && COMMUNITY_BOARD_KEY.equalsIgnoreCase(board.getBoCode().trim());
    }

    private boolean shouldApplyLocationScopedFilter(Board board, BoardSearchRequest searchRequest) {
        if (isNeighborBoard(board)) {
            return true;
        }
        return isCommunityBoard(board) && searchRequest != null && searchRequest.isNeighborLocationScope();
    }

    private String resolveRequestedPostScope(BoardItemRequest request) {
        if (request == null) {
            return null;
        }
        return firstNonBlank(request.getPostScope(), request.getLocationScope());
    }

    private String normalizePostScope(String rawScope) {
        if (rawScope == null || rawScope.isBlank()) {
            return "all";
        }
        return "neighbor".equalsIgnoreCase(rawScope.trim()) ? "neighbor" : "all";
    }

    private boolean isNeighborPostScope(String postScope) {
        return "neighbor".equals(normalizePostScope(postScope));
    }

    private void validateCommunityWriteRules(Board board, BoardItemRequest request, String postScope) {
        if (!isCommunityBoard(board) || request == null) {
            return;
        }
        String normalizedCategory = normalizeCategory(request.getCategory());
        validateCommunityCategoryForPostScope(normalizedCategory, postScope);
        if ("local_review".equals(normalizedCategory)) {
            validateLocalReviewPlaceRequired(request);
        }
    }

    private void validateCommunityUpdateRules(Board board, BoardItem item, BoardItemRequest request) {
        if (!isCommunityBoard(board) || item == null || request == null) {
            return;
        }
        String itemPostScope = normalizePostScope(item.getLocationScope());
        String normalizedCategory = normalizeCategory(request.getCategory());
        validateCommunityCategoryForPostScope(normalizedCategory, itemPostScope);
        if ("local_review".equals(normalizedCategory)) {
            validateLocalReviewPlaceRequired(request);
        }
    }

    private void validateCommunityCategoryForPostScope(String category, String postScope) {
        if (!hasText(category)) {
            return;
        }

        String normalizedCategory = normalizeCategory(category);
        if (LEGACY_COMPAT_HOSPITAL_CATEGORY.equals(normalizedCategory)) {
            return;
        }

        boolean valid = isNeighborPostScope(postScope)
                ? COMMUNITY_NEIGHBOR_CATEGORIES.contains(normalizedCategory)
                : COMMUNITY_ALL_CATEGORIES.contains(normalizedCategory);

        if (!valid) {
            throw new BoardException(BoardErrorCode.ITEM_SCOPE_CATEGORY_INVALID);
        }
    }

    private void validateLocalReviewPlaceRequired(BoardItemRequest request) {
        if (!hasValidPlace(request.getPlaceName(), request.getPlaceAddress(), request.getPlaceLat(), request.getPlaceLng())) {
            throw new BoardException(BoardErrorCode.ITEM_PLACE_REQUIRED);
        }
    }

    private boolean hasValidPlace(String placeName, String placeAddress, Double placeLat, Double placeLng) {
        return hasText(placeName)
                && hasText(placeAddress)
                && isValidLatitude(placeLat)
                && isValidLongitude(placeLng);
    }

    private boolean isValidLatitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -90.0d && value <= 90.0d;
    }

    private boolean isValidLongitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -180.0d && value <= 180.0d;
    }

    private NeighborPostcodeScope validateCommunityNeighborPostPermission(Member member) {
        if (!hasText(member.getRegionName())) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }
        return resolveNeighborPostcodeScope(member);
    }

    private NeighborPostcodeScope buildOptionalCommunityLocationScope(Member member) {
        if (member == null || !hasText(member.getRegionName())) {
            return null;
        }
        try {
            return resolveNeighborPostcodeScope(member);
        } catch (BoardException e) {
            if (BoardErrorCode.NEIGHBOR_AUTH_REQUIRED.getCode().equals(e.getCode())) {
                return null;
            }
            throw e;
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

    private ApiResponse<Integer> likeItemInternal(UUID memberId, Board board, Member member, Long itemId) {
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);

        validateReadPermission(board, member);
        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
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

    private ApiResponse<Integer> unlikeItemInternal(UUID memberId, Board board, Member member, Long itemId) {
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);

        validateReadPermission(board, member);
        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
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
        BoardItem item = validateItemInBoard(board.getBoSeq(), itemId);

        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);

        // 동네 게시판인 경우 동네 검증
        if ("Y".equals(board.getBoNeighborYn())) {
            validateNeighborAccess(board, member, item);
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
        BoardSearchRequest normalizedRequest = searchRequest == null ? BoardSearchRequest.builder().build()
                : searchRequest;

        String category = normalizeCategory(normalizedRequest.getCategory());
        boolean communityUrgentSlotRequest = isCommunityUrgentSlotRequest(board, normalizedRequest, category);

        // 동네 게시판 또는 커뮤니티의 내 동네 보기인 경우 위치 스코프 적용
        NeighborPostcodeScope postcodeScope = null;
        if (shouldApplyLocationScopedFilter(board, normalizedRequest)) {
            try {
                postcodeScope = resolveNeighborPostcodeScope(member);
            } catch (BoardException e) {
                // Community list should degrade to "all" scope when profile location is
                // stale/missing.
                if (isCommunityBoard(board)
                        && BoardErrorCode.NEIGHBOR_AUTH_REQUIRED.getCode().equals(e.getCode())) {
                    if (communityUrgentSlotRequest) {
                        return ApiResponse.success("게시글 목록 조회 성공", emptyItemListResult(normalizedRequest));
                    }
                    log.info("Fallback to all-scope community list due to missing location for member: {}", memberId);
                } else {
                    throw e;
                }
            }
        }

        Long boardId = board.getBoSeq();
        String regionCode = postcodeScope == null ? null : postcodeScope.regionCode();
        Integer postcode = postcodeScope == null ? null : postcodeScope.exactPostcode();
        Integer postcodePrefix = postcodeScope == null ? null : postcodeScope.legacyPrefix();

        List<BoardItemListDto> fixedDtos = Collections.emptyList();
        List<BoardItemListDto> popularDtos = Collections.emptyList();
        List<BoardItemListDto> urgentDtos = Collections.emptyList();

        if (normalizedRequest.isIncludeHighlights()) {
            // 고정글 조회
            fixedDtos = boardMapper.getFixedItems(boardId, regionCode, postcode, postcodePrefix, category,
                    normalizedRequest.getLocationScope(), memberId);
            attachThumbnailUrls(fixedDtos);
            attachLocationMetadata(fixedDtos, member);
            Set<Long> fixedIds = fixedDtos.stream().map(BoardItemListDto::getId).collect(Collectors.toSet());

            // 인기글 조회 (조회수+공감수 상위 3건, 고정글 제외)
            popularDtos = boardMapper.getPopularItems(boardId, regionCode, postcode, postcodePrefix, category,
                    normalizedRequest.getLocationScope(),
                    memberId);
            attachThumbnailUrls(popularDtos);
            popularDtos = popularDtos.stream()
                    .filter(item -> !fixedIds.contains(item.getId()))
                    .peek(item -> item.setPopular(true))
                    .collect(Collectors.toList());
            attachLocationMetadata(popularDtos, member);

            if (shouldIncludeCommunityUrgentHighlights(board, normalizedRequest, category)) {
                urgentDtos = boardMapper.searchItems(
                        boardId,
                        regionCode,
                        postcode,
                        postcodePrefix,
                        "urgent",
                        memberId,
                        "titleContent",
                        null,
                        normalizedRequest.getLocationScope(),
                        "latest",
                        0,
                        3);
                attachThumbnailUrls(urgentDtos);
                attachLocationMetadata(urgentDtos, member);
            }
        }

        // 일반글 조회
        Map<String, Object> searchResult = searchItems(
                boardId,
                postcodeScope,
                normalizedRequest,
                category,
                memberId,
                communityUrgentSlotRequest);
        @SuppressWarnings("unchecked")
        List<BoardItemListDto> normalDtos = (List<BoardItemListDto>) searchResult.get("items");
        attachThumbnailUrls(normalDtos);
        attachLocationMetadata(normalDtos, member);

        Map<String, Object> result = new HashMap<>();
        result.put("fixedItems", fixedDtos);
        result.put("popularItems", popularDtos);
        result.put("urgentItems", urgentDtos);
        result.put("items", normalDtos);
        result.put("totalElements", searchResult.get("totalElements"));
        result.put("totalPages", searchResult.get("totalPages"));
        result.put("currentPage", searchResult.get("currentPage"));
        result.put("size", searchResult.get("size"));

        return ApiResponse.success("게시글 목록 조회 성공", result);
    }

    private Map<String, Object> searchItems(Long boardId, NeighborPostcodeScope postcodeScope,
            BoardSearchRequest searchRequest,
            String category, UUID memberId, boolean communityUrgentSlotRequest) {
        String keyword = searchRequest.getKeyword();
        String searchType = searchRequest.getSearchType();
        int page = searchRequest.getPage();
        int size = searchRequest.getSize();
        int offset = page * size;
        String locationScope = searchRequest.getLocationScope();
        String effectiveCategory = communityUrgentSlotRequest ? "urgent" : category;
        String sort = (communityUrgentSlotRequest || searchRequest.isNeighborLocationScope())
                ? "latest"
                : searchRequest.getSort();

        // searchType이 없으면 기본값 titleContent
        if (searchType == null || searchType.isBlank()) {
            searchType = "titleContent";
        }

        // Mapper로 검색
        Integer postcode = postcodeScope == null ? null : postcodeScope.exactPostcode();
        Integer postcodePrefix = postcodeScope == null ? null : postcodeScope.legacyPrefix();
        String regionCode = postcodeScope == null ? null : postcodeScope.regionCode();
        List<BoardItemListDto> items = boardMapper.searchItems(
                boardId,
                regionCode,
                postcode,
                postcodePrefix,
                effectiveCategory,
                memberId,
                searchType,
                keyword,
                locationScope,
                sort,
                offset,
                size);

        if (communityUrgentSlotRequest) {
            List<BoardItemListDto> urgentSlotItems = filterCommunityUrgentSlotItems(
                    items,
                    LocalDateTime.now().minusHours(COMMUNITY_URGENT_SLOT_WINDOW_HOURS));
            return buildSearchResult(urgentSlotItems, urgentSlotItems.size(), 0, COMMUNITY_URGENT_SLOT_MAX_ITEMS);
        }

        int totalCount = boardMapper.countSearchItems(
                boardId,
                regionCode,
                postcode,
                postcodePrefix,
                effectiveCategory,
                searchType,
                keyword,
                locationScope);
        return buildSearchResult(items, totalCount, page, size);
    }

    private Map<String, Object> buildSearchResult(List<BoardItemListDto> items, int totalCount, int page, int size) {
        List<BoardItemListDto> safeItems = items == null ? Collections.emptyList() : items;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new HashMap<>();
        result.put("items", safeItems);
        result.put("totalElements", totalCount);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);
        return result;
    }

    private Map<String, Object> emptyItemListResult(BoardSearchRequest searchRequest) {
        int page = searchRequest.isUrgentSlot() ? 0 : searchRequest.getPage();
        int size = searchRequest.isUrgentSlot() ? COMMUNITY_URGENT_SLOT_MAX_ITEMS : searchRequest.getSize();
        Map<String, Object> result = new HashMap<>();
        result.put("fixedItems", Collections.emptyList());
        result.put("popularItems", Collections.emptyList());
        result.put("urgentItems", Collections.emptyList());
        result.putAll(buildSearchResult(Collections.emptyList(), 0, page, size));
        return result;
    }

    private List<BoardItemListDto> filterCommunityUrgentSlotItems(List<BoardItemListDto> items, LocalDateTime cutoff) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getCategory() != null && "urgent".equalsIgnoreCase(item.getCategory().trim()))
                .filter(item -> !item.isUrgentResolved())
                .filter(item -> item.getRegDate() != null && !item.getRegDate().isBefore(cutoff))
                .sorted((left, right) -> right.getRegDate().compareTo(left.getRegDate()))
                .limit(COMMUNITY_URGENT_SLOT_MAX_ITEMS)
                .collect(Collectors.toList());
    }

    private boolean isCommunityUrgentSlotRequest(Board board, BoardSearchRequest searchRequest, String category) {
        if (!isCommunityBoard(board) || searchRequest == null || !searchRequest.isUrgentSlot()) {
            return false;
        }
        if (!searchRequest.isNeighborLocationScope()) {
            return false;
        }
        if (category != null && !category.isBlank() && !"urgent".equals(category)) {
            return false;
        }
        String keyword = searchRequest.getKeyword();
        return keyword == null || keyword.isBlank();
    }

    private boolean shouldIncludeCommunityUrgentHighlights(Board board, BoardSearchRequest searchRequest, String category) {
        if (!isCommunityBoard(board) || searchRequest == null) {
            return false;
        }
        if (!searchRequest.isNeighborLocationScope()) {
            return false;
        }
        if (category != null && !category.isBlank()) {
            return false;
        }
        String keyword = searchRequest.getKeyword();
        return keyword == null || keyword.isBlank();
    }

    private record NeighborPostcodeScope(String regionCode, Integer exactPostcode, Integer legacyPrefix) {
    }

    private BoardItemDto toDto(BoardItem item, List<BoardFile> files, int commentCount, boolean liked, UUID memberId) {
        Member member = getMember(memberId);
        return toDto(item, files, commentCount, liked, member);
    }

    private BoardItemDto toDto(BoardItem item, List<BoardFile> files, int commentCount, boolean liked, Member viewer) {
        Member author = memberRepository.findById(item.getRegId()).orElse(null);
        String authorName = author != null && hasText(author.getName()) ? author.getName() : "Unknown";
        String authorRegionName = RegionLabelFormatter.normalize(firstNonBlank(
                item.getRegUserRegionName(),
                author == null ? null : author.getRegionName()));

        Board board = boardRepository.findById(item.getBoSeq()).orElse(null);
        String boardTitle = board != null ? board.getBoTitle() : "";
        String boardSlug = board != null ? board.getBoSlug() : "";

        List<BoardFileDto> fileDtos = files.stream()
                .map(this::toFileDto)
                .collect(Collectors.toList());

        return BoardItemDto.builder()
                .id(item.getBiSeq())
                .boardId(item.getBoSeq())
                .boardSlug(boardSlug)
                .boardTitle(boardTitle)
                .title(item.getTitle())
                .content(item.getContent())
                .category(item.getBiCategory())
                .postScope(normalizePostScope(item.getLocationScope()))
                .readCount(item.getReadCount())
                .likeCount(item.getLikeCount())
                .fixYn(item.getFixYn())
                .placeName(item.getPlaceName())
                .placeAddress(item.getPlaceAddress())
                .placeLat(item.getPlaceLat())
                .placeLng(item.getPlaceLng())
                .urgentResolved(isResolvedFlag(item.getUrgentResolvedYn()))
                .urgentResolvedDate(item.getUrgentResolvedDate())
                .regUserPostcode(item.getRegUserPostcode())
                .regId(item.getRegId())
                .regUserName(authorName)
                .regUserRegionName(authorRegionName)
                .regUserRegionDongLabel(RegionLabelFormatter.toDongLabel(authorRegionName))
                .sameNeighborhood(RegionLabelFormatter.isSameNeighborhood(authorRegionName, viewer.getRegionName()))
                .regDate(item.getRegDate())
                .updateId(item.getUpdateId())
                .updateDate(item.getUpdateDate())
                .files(fileDtos)
                .commentCount(commentCount)
                .liked(liked)
                .isAuthor(viewer.getId().equals(item.getRegId()) || "ADMIN".equals(viewer.getRole().name()))
                .build();
    }

    private void attachLocationMetadata(List<BoardItemListDto> items, Member viewer) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String viewerRegionName = viewer == null ? null : viewer.getRegionName();
        for (BoardItemListDto item : items) {
            if (item == null) {
                continue;
            }
            item.setPostScope(normalizePostScope(item.getPostScope()));
            String normalizedRegionName = RegionLabelFormatter.normalize(item.getRegUserRegionName());
            item.setRegUserRegionName(normalizedRegionName);
            item.setRegUserRegionDongLabel(RegionLabelFormatter.toDongLabel(normalizedRegionName));
            item.setSameNeighborhood(RegionLabelFormatter.isSameNeighborhood(normalizedRegionName, viewerRegionName));
        }
    }

    private void attachThumbnailUrls(List<BoardItemListDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> itemIds = items.stream()
                .filter(Objects::nonNull)
                .filter(BoardItemListDto::isHasFile)
                .map(BoardItemListDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (itemIds.isEmpty()) {
            return;
        }

        List<BoardFile> files = boardFileRepository.findByBiSeqInOrderByRegDateAsc(itemIds);
        Map<Long, String> optimizedThumbnailUrlByFileId = boardImageOptimizationService.getPreferredThumbnailUrlsByFileIds(
                files.stream()
                        .filter(Objects::nonNull)
                        .filter(file -> isImageExtension(file.getBfExtension()))
                        .map(BoardFile::getBfSeq)
                        .filter(Objects::nonNull)
                        .toList()
        );
        Map<Long, BoardFileDto.ImageVariantSet> optimizedThumbnailVariantByFileId = boardImageOptimizationService.getPreferredThumbnailVariantSetsByFileIds(
                files.stream()
                        .filter(Objects::nonNull)
                        .filter(file -> isImageExtension(file.getBfExtension()))
                        .map(BoardFile::getBfSeq)
                        .filter(Objects::nonNull)
                        .toList()
        );
        if (optimizedThumbnailUrlByFileId == null) {
            optimizedThumbnailUrlByFileId = Collections.emptyMap();
        }
        if (optimizedThumbnailVariantByFileId == null) {
            optimizedThumbnailVariantByFileId = Collections.emptyMap();
        }
        Map<Long, String> thumbnailUrlByItemId = new HashMap<>();
        Map<Long, BoardFileDto.ImageVariantSet> thumbnailVariantByItemId = new HashMap<>();

        for (BoardFile file : files) {
            Long itemId = file.getBiSeq();
            if (itemId == null || thumbnailUrlByItemId.containsKey(itemId)) {
                continue;
            }
            if (!isImageExtension(file.getBfExtension())) {
                continue;
            }
            String optimizedThumbnailUrl = optimizedThumbnailUrlByFileId.get(file.getBfSeq());
            if (optimizedThumbnailUrl != null) {
                thumbnailUrlByItemId.put(itemId, optimizedThumbnailUrl);
                BoardFileDto.ImageVariantSet optimizedVariantSet = optimizedThumbnailVariantByFileId.get(file.getBfSeq());
                if (optimizedVariantSet != null) {
                    thumbnailVariantByItemId.put(itemId, optimizedVariantSet);
                }
                continue;
            }
            String signedUrl = getSignedUrl(file.getBfPath());
            if (signedUrl != null) {
                thumbnailUrlByItemId.put(itemId, signedUrl);
            }
        }

        items.forEach(item -> {
            item.setThumbnailUrl(thumbnailUrlByItemId.get(item.getId()));
            applyThumbnailVariant(item, thumbnailVariantByItemId.get(item.getId()));
        });
    }

    private void applyThumbnailVariant(BoardItemListDto item, BoardFileDto.ImageVariantSet variantSet) {
        if (item == null) {
            return;
        }

        if (variantSet == null) {
            item.setThumbnailAvifUrl(null);
            item.setThumbnailWebpUrl(null);
            item.setThumbnailJpegUrl(null);
            item.setThumbnailPngUrl(null);
            item.setThumbnailWidth(null);
            item.setThumbnailHeight(null);
            return;
        }

        item.setThumbnailAvifUrl(variantSet.getAvifUrl());
        item.setThumbnailWebpUrl(variantSet.getWebpUrl());
        item.setThumbnailJpegUrl(variantSet.getJpegUrl());
        item.setThumbnailPngUrl(variantSet.getPngUrl());
        item.setThumbnailWidth(variantSet.getWidth());
        item.setThumbnailHeight(variantSet.getHeight());
    }

    private boolean isImageExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private String getSignedUrl(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        try {
            return storageService.getSignedUrl(filePath, SIGNED_URL_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to issue signed URL for filePath: {}", filePath, e);
            return null;
        }
    }

    private BoardFileDto toFileDto(BoardFile file) {
        BoardFileDto dto = BoardFileDto.builder()
                .id(file.getBfSeq())
                .itemId(file.getBiSeq())
                .orgFilename(file.getOrgFilename())
                .fileName(file.getBfName())
                .filePath(file.getBfPath())
                .extension(file.getBfExtension())
                .fileSize(file.getBfSize())
                .regDate(file.getRegDate())
                .downloadUrl(getSignedUrl(file.getBfPath()))
                .build();
        boardImageOptimizationService.applyOptimizationFields(dto, file);
        return dto;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        // "육아광장" & "동네생활" & legacy categories
        if (!Set.of(
                "qna", "daily", "tip", "hospital", "urgent", // legacy
                "local_info", "local_review", "local_gathering", "local_share", // neighbor new
                "item_review", "info_share" // all new
        ).contains(normalized)) {
            throw new BoardException(BoardErrorCode.ITEM_CATEGORY_INVALID);
        }
        return normalized;
    }

    private boolean isResolvedFlag(String urgentResolvedYn) {
        return "Y".equalsIgnoreCase(firstNonBlank(urgentResolvedYn));
    }

    private void syncUrgentResolvedStateForCategory(BoardItem item, String normalizedCategory) {
        if (item == null) {
            return;
        }

        if (!"urgent".equals(normalizedCategory)) {
            item.setUrgentResolvedYn("N");
            item.setUrgentResolvedDate(null);
            return;
        }

        if (isResolvedFlag(item.getUrgentResolvedYn())) {
            item.setUrgentResolvedYn("Y");
            if (item.getUrgentResolvedDate() == null) {
                item.setUrgentResolvedDate(LocalDateTime.now());
            }
            return;
        }

        item.setUrgentResolvedYn("N");
        item.setUrgentResolvedDate(null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
