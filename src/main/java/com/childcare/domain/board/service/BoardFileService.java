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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardFileService {

    private final BoardRepository boardRepository;
    private final BoardItemRepository boardItemRepository;
    private final BoardFileRepository boardFileRepository;
    private final MemberRepository memberRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

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
    public ApiResponse<List<BoardFileDto>> uploadFiles(Long memberSeq, Long boardId, Long itemId, List<MultipartFile> files) {
        log.info("Upload files for item: {}, member: {}, file count: {}", itemId, memberSeq, files.size());

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 작성 권한 검증 (게시글 작성자 또는 ADMIN)
        Member member = getMember(memberSeq);
        validateFileUploadPermission(board, member, item.getRegUserSeq());

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
            BoardFileDto savedFile = saveFile(file, itemId, memberSeq, board.getBoCode());
            uploadedFiles.add(savedFile);
        }

        return ApiResponse.success("파일 업로드 성공", uploadedFiles);
    }

    /**
     * 파일 다운로드
     */
    public Resource downloadFile(Long memberSeq, Long boardId, Long itemId, Long fileId) {
        log.info("Download file: {} for item: {}, member: {}", fileId, itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        validateItem(itemId);

        // 읽기 권한 검증
        Member member = getMember(memberSeq);
        validateReadPermission(board, member);

        // 파일 조회
        BoardFile boardFile = boardFileRepository.findById(fileId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));

        // 게시글 일치 여부 확인
        if (!boardFile.getBiSeq().equals(itemId)) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }

        try {
            Path filePath = Paths.get(boardFile.getBfPath(), boardFile.getBfName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * 파일 삭제
     */
    @Transactional
    public ApiResponse<Void> deleteFile(Long memberSeq, Long boardId, Long itemId, Long fileId) {
        log.info("Delete file: {} for item: {}, member: {}", fileId, itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        BoardItem item = validateItem(itemId);

        // 삭제 권한 검증
        Member member = getMember(memberSeq);
        validateFileDeletePermission(board, member, item.getRegUserSeq());

        // 파일 조회
        BoardFile boardFile = boardFileRepository.findById(fileId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));

        // 게시글 일치 여부 확인
        if (!boardFile.getBiSeq().equals(itemId)) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }

        // 실제 파일 삭제
        try {
            Path filePath = Paths.get(boardFile.getBfPath(), boardFile.getBfName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", boardFile.getBfPath(), e);
        }

        // DB에서 삭제
        boardFileRepository.delete(boardFile);

        return ApiResponse.success("파일 삭제 성공", null);
    }

    /**
     * 파일 목록 조회
     */
    public ApiResponse<List<BoardFileDto>> getFiles(Long memberSeq, Long boardId, Long itemId) {
        log.info("Get files for item: {}, member: {}", itemId, memberSeq);

        // 게시판 및 게시글 조회
        Board board = validateBoard(boardId);
        validateItem(itemId);

        // 읽기 권한 검증
        Member member = getMember(memberSeq);
        validateReadPermission(board, member);

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

    private BoardFileDto saveFile(MultipartFile file, Long itemId, Long memberSeq, String boCode) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            // 저장 경로 생성 (uploads/board/{boCode}/yyyyMMdd/)
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path uploadPath = Paths.get(uploadDir, "board", boCode, datePath);
            Files.createDirectories(uploadPath);

            // 고유 파일명 생성
            String savedFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(savedFilename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // DB 저장
            BoardFile boardFile = BoardFile.builder()
                    .biSeq(itemId)
                    .orgFilename(originalFilename)
                    .bfName(savedFilename)
                    .bfPath(uploadPath.toString())
                    .bfExtension(extension)
                    .bfSize((int) file.getSize())
                    .regUserSeq(memberSeq)
                    .regDate(LocalDateTime.now())
                    .build();

            BoardFile savedFile = boardFileRepository.save(boardFile);

            return toDto(savedFile);

        } catch (IOException e) {
            log.error("Failed to save file: {}", file.getOriginalFilename(), e);
            throw new BoardException(BoardErrorCode.FILE_UPLOAD_FAILED);
        }
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

    private Member getMember(Long memberSeq) {
        return memberRepository.findByMbSeq(memberSeq)
                .orElseThrow(() -> new BoardException(BoardErrorCode.READ_PERMISSION_DENIED));
    }

    private void validateReadPermission(Board board, Member member) {
        if ("ADMIN".equals(board.getBoReadAuth()) && !"ADMIN".equals(member.getRole().name())) {
            throw new BoardException(BoardErrorCode.READ_PERMISSION_DENIED);
        }
    }

    private void validateFileUploadPermission(Board board, Member member, Long itemAuthorSeq) {
        // ADMIN은 항상 업로드 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // 게시글 작성자만 파일 업로드 가능
        if (!member.getMbSeq().equals(itemAuthorSeq)) {
            throw new BoardException(BoardErrorCode.FILE_UPLOAD_DENIED);
        }
    }

    private void validateFileDeletePermission(Board board, Member member, Long itemAuthorSeq) {
        // ADMIN은 항상 삭제 가능
        if ("ADMIN".equals(member.getRole().name())) {
            return;
        }
        // 게시글 작성자만 파일 삭제 가능
        if (!member.getMbSeq().equals(itemAuthorSeq)) {
            throw new BoardException(BoardErrorCode.FILE_DELETE_DENIED);
        }
    }

    private BoardFileDto toDto(BoardFile file) {
        return BoardFileDto.builder()
                .id(file.getBfSeq())
                .itemId(file.getBiSeq())
                .orgFilename(file.getOrgFilename())
                .fileName(file.getBfName())
                .filePath(file.getBfPath())
                .extension(file.getBfExtension())
                .fileSize(file.getBfSize())
                .regDate(file.getRegDate())
                .build();
    }
}
