package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardFile;
import com.childcare.domain.board.entity.BoardItem;
import com.childcare.domain.board.repository.BoardFileRepository;
import com.childcare.domain.board.repository.BoardItemRepository;
import com.childcare.domain.board.repository.BoardRepository;
import com.childcare.domain.member.entity.Member;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.childcare.global.service.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BoardFileService {
    private static final Logger log = LoggerFactory.getLogger(BoardFileService.class);

    private final BoardRepository boardRepository;
    private final BoardItemRepository boardItemRepository;
    private final BoardFileRepository boardFileRepository;
    private final MemberRepository memberRepository;
    private final SupabaseStorageService storageService;

    public BoardFileService(
            BoardRepository boardRepository,
            BoardItemRepository boardItemRepository,
            BoardFileRepository boardFileRepository,
            MemberRepository memberRepository,
            SupabaseStorageService storageService
    ) {
        this.boardRepository = boardRepository;
        this.boardItemRepository = boardItemRepository;
        this.boardFileRepository = boardFileRepository;
        this.memberRepository = memberRepository;
        this.storageService = storageService;
    }

    // 기본 허용 확장자
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "hwp", "zip"
    );

    // 기본 파일 크기 제한 (10MB)
    private static final int DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 기본 파일 개수 제한
    private static final int DEFAULT_MAX_FILE_COUNT = 5;

    /**
     * 파일 업로드
     */
    @Transactional
    public ApiResponse<List<BoardFileDto>> uploadFiles(UUID memberId, Long boardId, Long itemId, List<MultipartFile> files) {
        log.info("Upload files for item: {}, member: {}, file count: {}", itemId, memberId, files.size());

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 작성 권한 검증 (게시글 작성자 또는 ADMIN)
        Member member = getMember(memberId);
        validateNeighborAccess(board, member, item);
        validateFileUploadPermission(board, member, item.getRegId());

        // 파일 개수 제한 확인
        int maxFileCount = parseMaxFileCount(board.getBoFileCount());
        List<BoardFile> existingFiles = boardFileRepository.findByBiSeq(itemId);
        if (existingFiles.size() + files.size() > maxFileCount) {
            throw new BoardException(BoardErrorCode.FILE_COUNT_EXCEEDED);
        }

        // 허용 확장자 목록
        List<String> allowedExtensions = parseAllowedExtensions(board.getBoFileExtension());

        // 파일 크기 제한
        int maxFileSize = board.getBoFileSize() != null ? board.getBoFileSize() : DEFAULT_MAX_FILE_SIZE;

        List<BoardFileDto> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // 파일 크기 검증
            if (file.getSize() > maxFileSize) {
                throw new BoardException(BoardErrorCode.FILE_SIZE_EXCEEDED);
            }

            // 확장자 검증
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            if (!allowedExtensions.contains(extension.toLowerCase())) {
                throw new BoardException(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED);
            }

            // 파일 저장
            BoardFileDto savedFile = saveFile(file, itemId, memberId, board.getBoCode());
            uploadedFiles.add(savedFile);
        }

        return ApiResponse.success("파일 업로드 성공", uploadedFiles);
    }

    /**
     * 파일 다운로드 URL 조회
     * @return 서명된 다운로드 URL (1시간 유효)
     */
    public String getDownloadUrl(UUID memberId, Long boardId, Long itemId, Long fileId) {
        log.info("Get download URL for file: {} item: {}, member: {}", fileId, itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);
        validateNeighborAccess(board, member, item);

        // 파일 조회
        BoardFile boardFile = boardFileRepository.findById(fileId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));

        // 게시글 일치 여부 확인
        if (!boardFile.getBiSeq().equals(itemId)) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }

        // 서명된 URL 반환 (1시간 = 3600초)
        return storageService.getSignedUrl(boardFile.getBfPath(), 3600);
    }

    /**
     * 파일 삭제
     */
    @Transactional
    public ApiResponse<Void> deleteFile(UUID memberId, Long boardId, Long itemId, Long fileId) {
        log.info("Delete file: {} for item: {}, member: {}", fileId, itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 삭제 권한 검증
        Member member = getMember(memberId);
        validateNeighborAccess(board, member, item);
        validateFileDeletePermission(board, member, item.getRegId());

        // 파일 조회
        BoardFile boardFile = boardFileRepository.findById(fileId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));

        // 게시글 일치 여부 확인
        if (!boardFile.getBiSeq().equals(itemId)) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }

        // Supabase Storage에서 파일 삭제
        storageService.deleteFile(boardFile.getBfPath());

        // DB에서 삭제
        boardFileRepository.delete(boardFile);

        return ApiResponse.success("파일 삭제 성공", null);
    }

    /**
     * 파일 목록 조회
     */
    public ApiResponse<List<BoardFileDto>> getFiles(UUID memberId, Long boardId, Long itemId) {
        log.info("Get files for item: {}, member: {}", itemId, memberId);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItemInBoard(boardId, itemId);

        // 읽기 권한 검증
        Member member = getMember(memberId);
        validateReadPermission(board, member);
        validateNeighborAccess(board, member, item);

        List<BoardFile> files = boardFileRepository.findByBiSeq(itemId);
        List<BoardFileDto> fileDtos = files.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("파일 목록 조회 성공", fileDtos);
    }

    /**
     * 파일 정보 조회 (다운로드용)
     */
    public BoardFile getFileInfo(Long fileId) {
        return boardFileRepository.findById(fileId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));
    }

    private BoardFileDto saveFile(MultipartFile file, Long itemId, UUID memberId, String boCode) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // 저장 경로 생성 (board/{boCode}/yyyyMMdd/)
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String savedFilename = UUID.randomUUID().toString() + "." + extension;
        String storagePath = "board/" + boCode + "/" + datePath + "/" + savedFilename;

        // Supabase Storage에 파일 업로드
        storageService.uploadFile(file, storagePath);

        // DB 저장
        BoardFile boardFile = BoardFile.builder()
                .biSeq(itemId)
                .orgFilename(originalFilename)
                .bfName(savedFilename)
                .bfPath(storagePath)  // Supabase Storage 경로 저장
                .bfExtension(extension)
                .bfSize((int) file.getSize())
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        BoardFile savedFile = boardFileRepository.save(boardFile);

        return toDto(savedFile);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private int parseMaxFileCount(String fileCountStr) {
        if (fileCountStr == null || fileCountStr.isBlank()) {
            return DEFAULT_MAX_FILE_COUNT;
        }
        try {
            return Integer.parseInt(fileCountStr);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_FILE_COUNT;
        }
    }

    private List<String> parseAllowedExtensions(String extensionStr) {
        if (extensionStr == null || extensionStr.isBlank()) {
            return DEFAULT_ALLOWED_EXTENSIONS;
        }
        return Arrays.stream(extensionStr.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

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

    private void validateFileUploadPermission(Board board, Member member, UUID itemAuthorId) {
        // ADMIN은 항상 업로드 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // 게시글 작성자만 파일 업로드 가능
        if (!member.getId().equals(itemAuthorId)) {
            throw new BoardException(BoardErrorCode.FILE_UPLOAD_DENIED);
        }
    }

    private void validateFileDeletePermission(Board board, Member member, UUID itemAuthorId) {
        // ADMIN은 항상 삭제 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // 게시글 작성자만 파일 삭제 가능
        if (!member.getId().equals(itemAuthorId)) {
            throw new BoardException(BoardErrorCode.FILE_DELETE_DENIED);
        }
    }

    private void validateNeighborAccess(Board board, Member member, BoardItem item) {
        if (!"Y".equals(board.getBoNeighborYn())) {
            return;
        }

        NeighborPostcodeScope scope = resolveNeighborPostcodeScope(member);
        if (!isNeighborMatched(item.getRegUserRegionCode(), item.getRegUserPostcode(), scope)) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_ACCESS_DENIED);
        }
    }

    private BoardFileDto toDto(BoardFile file) {
        // 서명된 다운로드 URL 생성 (1시간 유효)
        String downloadUrl = storageService.getSignedUrl(file.getBfPath(), 3600);

        return BoardFileDto.builder()
                .id(file.getBfSeq())
                .itemId(file.getBiSeq())
                .orgFilename(file.getOrgFilename())
                .fileName(file.getBfName())
                .filePath(file.getBfPath())
                .extension(file.getBfExtension())
                .fileSize(file.getBfSize())
                .regDate(file.getRegDate())
                .downloadUrl(downloadUrl)
                .build();
    }

    private NeighborPostcodeScope resolveNeighborPostcodeScope(Member member) {
        if (member.getPostcode() == null || member.getPostcode().isBlank()) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }

        String normalizedRegionCode = normalizeRegionCode(member.getRegionCode());
        String digitsOnly = member.getPostcode().replaceAll("\\D", "");
        if (digitsOnly.length() < 3) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }

        try {
            String exactRaw = digitsOnly.length() >= 5 ? digitsOnly.substring(0, 5) : digitsOnly;
            String prefixRaw = digitsOnly.substring(0, 3);

            int exactPostcode = Integer.parseInt(exactRaw);
            int legacyPrefix = Integer.parseInt(prefixRaw);
            return new NeighborPostcodeScope(normalizedRegionCode, exactPostcode, legacyPrefix);
        } catch (NumberFormatException e) {
            throw new BoardException(BoardErrorCode.NEIGHBOR_AUTH_REQUIRED);
        }
    }

    private boolean isNeighborMatched(String itemRegionCode, Integer itemPostcode, NeighborPostcodeScope scope) {
        if (scope == null) {
            return false;
        }

        String normalizedItemRegionCode = normalizeRegionCode(itemRegionCode);
        if (hasText(scope.regionCode()) && hasText(normalizedItemRegionCode)) {
            return scope.regionCode().equals(normalizedItemRegionCode);
        }

        if (itemPostcode == null) {
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

    private record NeighborPostcodeScope(String regionCode, int exactPostcode, int legacyPrefix) {
    }
}
