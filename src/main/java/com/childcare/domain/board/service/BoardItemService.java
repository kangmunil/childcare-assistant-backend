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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public ApiResponse<Map<String, Object>> getItemList(Long memberSeq, Long boardId, BoardSearchRequest searchRequest) {
        log.info("Get item list for board: {}, member: {}", boardId, memberSeq);

        // 게시판 조회 및 검증
        Board board = validateBoard(boardId);

        // 읽기 권한 검증
        Member member = getMember(memberSeq);
        validateReadPermission(board, member);

        // 동네 게시판인 경우 우편번호 검증
        Integer userPostcode = null;
        if ("Y".equals(board.getBoNeighborYn())) {
            userPostcode = validateNeighborAuth(member);
        }

        // 고정글 조회
        List<BoardItemListDto> fixedDtos = boardMapper.getFixedItems(boardId);
        Set<Long> fixedIds = fixedDtos.stream().map(BoardItemListDto::getId).collect(Collectors.toSet());

        // 인기글 조회 (조회수+공감수 상위 3건, 고정글 제외)
        List<BoardItemListDto> popularDtos = boardMapper.getPopularItems(boardId);
        popularDtos = popularDtos.stream()
                .filter(item -> !fixedIds.contains(item.getId()))
                .peek(item -> item.setPopular(true))
                .collect(Collectors.toList());

        // 인기글 제외용 ID Set
        Set<Long> popularIds = popularDtos.stream().map(BoardItemListDto::getId).collect(Collectors.toSet());

        // 일반글 조회
        Map<String, Object> searchResult = searchItems(boardId, userPostcode, searchRequest);
        @SuppressWarnings("unchecked")
        List<BoardItemListDto> normalDtos = (List<BoardItemListDto>) searchResult.get("items");

        // 고정글, 인기글 제외
        normalDtos = normalDtos.stream()
                .filter(item -> !fixedIds.contains(item.getId()) && !popularIds.contains(item.getId()))
                .collect(Collectors.toList());

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

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public ApiResponse<BoardItemDto> getItem(Long memberSeq, Long boardId, Long itemId) {
        log.info("Get item: {} for board: {}, member: {}", itemId, boardId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 읽기 권한 검증
        Member member = getMember(memberSeq);
        validateReadPermission(board, member);

        // 동네 게시판인 경우 동네 검증
        if ("Y".equals(board.getBoNeighborYn())) {
            Integer userPostcode = validateNeighborAuth(member);
            if (!userPostcode.equals(item.getRegUserPostcode())) {
                throw new BoardException(BoardErrorCode.NEIGHBOR_ACCESS_DENIED);
            }
        }

        // 조회수 증가 (중복 방지)
        increaseReadCount(itemId, memberSeq);

        // 첨부파일 조회
        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);

        // 댓글 수 조회
        long commentCount = boardCommentRepository.countByBiSeqAndDeleteYnIsNull(itemId);

        // 공감 여부
        boolean liked = boardItemLikeRepository.existsByBiSeqAndMbSeq(itemId, memberSeq);

        // DTO 변환
        BoardItemDto dto = toDto(item, files, (int) commentCount, liked, memberSeq);

        return ApiResponse.success("게시글 조회 성공", dto);
    }

    /**
     * 게시글 작성
     */
    @Transactional
    public ApiResponse<BoardItemDto> createItem(Long memberSeq, Long boardId, BoardItemRequest request) {
        log.info("Create item for board: {}, member: {}", boardId, memberSeq);

        // 게시판 조회 및 검증
        Board board = validateBoard(boardId);

        // 작성 권한 검증
        Member member = getMember(memberSeq);
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
                .boSeq(boardId)
                .title(request.getTitle())
                .content(request.getContent())
                .readCount(0)
                .likeCount(0)
                .fixYn(fixYn)
                .regUserPostcode(userPostcode)
                .regUserSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .build();

        BoardItem savedItem = boardItemRepository.save(item);

        BoardItemDto dto = toDto(savedItem, Collections.emptyList(), 0, false, memberSeq);

        return ApiResponse.success("게시글 작성 성공", dto);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public ApiResponse<BoardItemDto> updateItem(Long memberSeq, Long boardId, Long itemId, BoardItemRequest request) {
        log.info("Update item: {} for board: {}, member: {}", itemId, boardId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 수정 권한 검증
        Member member = getMember(memberSeq);
        validateModifyPermission(board, member, item.getRegUserSeq());

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
        item.setUpdateUserSeq(memberSeq);
        item.setUpdateDate(LocalDateTime.now());

        BoardItem savedItem = boardItemRepository.save(item);

        // 첨부파일 조회
        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);
        long commentCount = boardCommentRepository.countByBiSeqAndDeleteYnIsNull(itemId);
        boolean liked = boardItemLikeRepository.existsByBiSeqAndMbSeq(itemId, memberSeq);

        BoardItemDto dto = toDto(savedItem, files, (int) commentCount, liked, memberSeq);

        return ApiResponse.success("게시글 수정 성공", dto);
    }

    /**
     * 게시글 삭제 (소프트 삭제)
     */
    @Transactional
    public ApiResponse<Void> deleteItem(Long memberSeq, Long boardId, Long itemId) {
        log.info("Delete item: {} for board: {}, member: {}", itemId, boardId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 삭제 권한 검증
        Member member = getMember(memberSeq);
        validateDeletePermission(board, member, item.getRegUserSeq());

        // 첨부파일 하드 삭제
        boardFileRepository.deleteByBiSeq(itemId);

        // 게시글 소프트 삭제
        item.setDeleteYn("Y");
        item.setDeleteUserSeq(String.valueOf(memberSeq));
        item.setDeleteDate(LocalDateTime.now());

        boardItemRepository.save(item);

        return ApiResponse.success("게시글 삭제 성공", null);
    }

    /**
     * 게시글 공감
     */
    @Transactional
    public ApiResponse<Integer> likeItem(Long memberSeq, Long boardId, Long itemId) {
        log.info("Like item: {} for member: {}", itemId, memberSeq);

        // 게시판 및 게시글 조회
        validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 이미 공감했는지 확인
        if (boardItemLikeRepository.existsByBiSeqAndMbSeq(itemId, memberSeq)) {
            throw new BoardException(BoardErrorCode.ALREADY_LIKED);
        }

        // 공감 저장
        BoardItemLike like = BoardItemLike.builder()
                .biSeq(itemId)
                .mbSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .build();
        boardItemLikeRepository.save(like);

        // 공감수 증가
        int newLikeCount = (item.getLikeCount() == null ? 0 : item.getLikeCount()) + 1;
        item.setLikeCount(newLikeCount);
        boardItemRepository.save(item);

        return ApiResponse.success("공감 성공", newLikeCount);
    }

    /**
     * 게시글 공감 취소
     */
    @Transactional
    public ApiResponse<Integer> unlikeItem(Long memberSeq, Long boardId, Long itemId) {
        log.info("Unlike item: {} for member: {}", itemId, memberSeq);

        // 게시판 및 게시글 조회
        validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 공감했는지 확인
        BoardItemLike like = boardItemLikeRepository.findByBiSeqAndMbSeq(itemId, memberSeq)
                .orElseThrow(() -> new BoardException(BoardErrorCode.NOT_LIKED));

        // 공감 삭제
        boardItemLikeRepository.delete(like);

        // 공감수 감소
        int newLikeCount = Math.max(0, (item.getLikeCount() == null ? 0 : item.getLikeCount()) - 1);
        item.setLikeCount(newLikeCount);
        boardItemRepository.save(item);

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
        // ADMIN은 항상 수정 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // USER 권한인 경우 작성자만 수정 가능
        if (!member.getMbSeq().equals(authorSeq)) {
            throw new BoardException(BoardErrorCode.MODIFY_PERMISSION_DENIED);
        }
    }

    private void validateDeletePermission(Board board, Member member, Long authorSeq) {
        // ADMIN 권한 게시판이면 ADMIN만 삭제 가능
        if ("ADMIN".equals(board.getBoDeleteAuth())) {
            if (!"ADMIN".equals(member.getRole().name())) {
                throw new BoardException(BoardErrorCode.DELETE_PERMISSION_DENIED);
            }
        } else {
            // USER 권한인 경우 작성자 또는 ADMIN만 삭제 가능
            if (!"ADMIN".equals(member.getRole().name()) && !member.getMbSeq().equals(authorSeq)) {
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
    }

    private void increaseReadCount(Long itemId, Long memberSeq) {
        if (!boardItemReadRepository.existsByBiSeqAndMbSeq(itemId, memberSeq)) {
            BoardItemRead read = BoardItemRead.builder()
                    .biSeq(itemId)
                    .mbSeq(memberSeq)
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

    private Map<String, Object> searchItems(Long boardId, Integer userPostcode, BoardSearchRequest searchRequest) {
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
        List<BoardItemListDto> items = boardMapper.searchItems(boardId, userPostcode, searchType, keyword, offset, size);
        int totalCount = boardMapper.countSearchItems(boardId, userPostcode, searchType, keyword);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("totalElements", totalCount);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    private BoardItemDto toDto(BoardItem item, List<BoardFile> files, int commentCount, boolean liked, Long memberSeq) {
        String authorName = memberRepository.findByMbSeq(item.getRegUserSeq())
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
                .readCount(item.getReadCount())
                .likeCount(item.getLikeCount())
                .fixYn(item.getFixYn())
                .regUserPostcode(item.getRegUserPostcode())
                .regUserSeq(item.getRegUserSeq())
                .regUserName(authorName)
                .regDate(item.getRegDate())
                .updateUserSeq(item.getUpdateUserSeq())
                .updateDate(item.getUpdateDate())
                .files(fileDtos)
                .commentCount(commentCount)
                .liked(liked)
                .isAuthor(item.getRegUserSeq().equals(memberSeq))
                .build();
    }
}
