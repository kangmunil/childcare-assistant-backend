package com.childcare.domain.child.service;

import com.childcare.domain.child.dto.ChildImageDto;
import com.childcare.domain.child.entity.ChildImage;
import com.childcare.domain.child.repository.ChildImageRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChildImageException;
import com.childcare.global.exception.ChildImageException.ChildImageErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import com.childcare.global.service.SupabaseStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildImageService {

    private final ChildImageRepository childImageRepository;
    private final ChildAccessValidator childAccessValidator;
    private final SupabaseStorageService storageService;

    @Value("${supabase.storage.child-image-bucket}")
    private String childImageBucket;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    );

    @PostConstruct
    public void init() {
        storageService.createBucketIfNotExists(childImageBucket, false);
    }

    @Transactional
    public ApiResponse<ChildImageDto> uploadImage(UUID memberId, Long childId, MultipartFile file) {
        log.info("Upload child image for child: {}, member: {}", childId, memberId);

        // 쓰기 권한 검증
        childAccessValidator.validateWriteAccess(memberId, childId);

        // 빈 파일 검증
        if (file.isEmpty()) {
            throw new ChildImageException(ChildImageErrorCode.EMPTY_FILE);
        }

        // 이미지 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ChildImageException(ChildImageErrorCode.INVALID_IMAGE_TYPE);
        }

        // 새 이미지 먼저 업로드
        ChildImageDto savedImage = saveImage(file, childId, memberId, extension);

        // 프로필 용이므로 이미지는 1건만 유지 -> 업로드 성공한 이미지를 제외한 기존 이미지 모두 삭제
        List<ChildImage> existingImages = childImageRepository.findAllByChSeq(childId);
        for (ChildImage old : existingImages) {
            if (!old.getCiSeq().equals(savedImage.getId())) {
                storageService.deleteFile(old.getCiPath(), childImageBucket);
                childImageRepository.delete(old);
                log.info("Deleted old child image: {}", old.getCiPath());
            }
        }

        return ApiResponse.success("이미지 업로드 성공", savedImage);
    }

    @Transactional
    public ApiResponse<Void> deleteImage(UUID memberId, Long childId) {
        log.info("Delete child image for child: {}, member: {}", childId, memberId);

        // 삭제 권한 검증
        childAccessValidator.validateDeleteAccess(memberId, childId);

        List<ChildImage> images = childImageRepository.findAllByChSeq(childId);
        if (images.isEmpty()) {
            throw new ChildImageException(ChildImageErrorCode.IMAGE_NOT_FOUND);
        }

        for (ChildImage image : images) {
            storageService.deleteFile(image.getCiPath(), childImageBucket);
            childImageRepository.delete(image);
        }

        return ApiResponse.success("이미지 삭제 성공", null);
    }

    private ChildImageDto saveImage(MultipartFile file, Long childId, UUID memberId, String extension) {
        String originalFilename = file.getOriginalFilename();

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String savedFilename = UUID.randomUUID().toString() + "." + extension;
        String storagePath = "child/" + childId + "/" + datePath + "/" + savedFilename; // (child/{childId}/yyyyMMdd/{uuid}.extension)

        storageService.uploadFile(file, storagePath, childImageBucket);

        ChildImage childImage = ChildImage.builder()
                .chSeq(childId)
                .orgFilename(originalFilename)
                .ciName(savedFilename)
                .ciPath(storagePath)
                .ciExtension(extension)
                .ciSize((int) file.getSize())
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        ChildImage saved = childImageRepository.save(childImage);

        return toDto(saved);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private ChildImageDto toDto(ChildImage image) {
        String downloadUrl = storageService.getSignedUrl(image.getCiPath(), 3600, childImageBucket);

        return ChildImageDto.builder()
                .id(image.getCiSeq())
                .childId(image.getChSeq())
                .orgFilename(image.getOrgFilename())
                .fileName(image.getCiName())
                .filePath(image.getCiPath())
                .extension(image.getCiExtension())
                .fileSize(image.getCiSize())
                .regDate(image.getRegDate())
                .downloadUrl(downloadUrl)
                .build();
    }
}
