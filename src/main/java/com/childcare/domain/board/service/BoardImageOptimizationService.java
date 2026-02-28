package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardFileDto;
import com.childcare.domain.board.entity.Board;
import com.childcare.domain.board.entity.BoardFile;
import com.childcare.domain.board.entity.BoardImageAsset;
import com.childcare.domain.board.entity.BoardImageJob;
import com.childcare.domain.board.entity.BoardImageVariant;
import com.childcare.domain.board.repository.BoardImageAssetRepository;
import com.childcare.domain.board.repository.BoardImageJobRepository;
import com.childcare.domain.board.repository.BoardImageVariantRepository;
import com.childcare.global.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardImageOptimizationService {
    private static final String COMMUNITY_BOARD_KEY = "community";
    private static final int SIGNED_URL_EXPIRE_SECONDS = 3600;
    private static final Set<String> COMMUNITY_ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> COMMUNITY_ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "image/jpg", "image/pjpeg"
    );
    private static final Set<String> LEGACY_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private final BoardImageAssetRepository boardImageAssetRepository;
    private final BoardImageVariantRepository boardImageVariantRepository;
    private final BoardImageJobRepository boardImageJobRepository;
    private final SupabaseStorageService storageService;

    public record OptimizationSnapshot(
            String mimeType,
            String optimizationStatus,
            BoardFileDto.ImageVariants imageVariants
    ) {
    }

    public boolean isCommunityBoard(Board board) {
        if (board == null) {
            return false;
        }
        String slug = board.getBoSlug();
        if (slug != null && COMMUNITY_BOARD_KEY.equalsIgnoreCase(slug.trim())) {
            return true;
        }
        String code = board.getBoCode();
        return code != null && COMMUNITY_BOARD_KEY.equalsIgnoreCase(code.trim());
    }

    public boolean isCommunityUploadImageExtensionAllowed(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return COMMUNITY_ALLOWED_IMAGE_EXTENSIONS.contains(extension.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isCommunityUploadImageMimeAllowed(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return true; // 일부 브라우저/클라이언트는 content-type을 비우고 보낼 수 있음
        }
        return COMMUNITY_ALLOWED_IMAGE_MIME_TYPES.contains(normalizeMimeType(mimeType));
    }

    @Transactional
    public void enqueueOptimizationIfEligible(Board board, BoardFile boardFile, MultipartFile sourceFile) {
        if (!isCommunityBoard(board) || boardFile == null) {
            return;
        }

        String extension = normalizeExtension(boardFile.getBfExtension());
        if (!isCommunityUploadImageExtensionAllowed(extension)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedMimeType = normalizeMimeType(sourceFile == null ? null : sourceFile.getContentType());

        BoardImageAsset asset = boardImageAssetRepository.findByBfSeq(boardFile.getBfSeq())
                .orElseGet(() -> BoardImageAsset.builder()
                        .bfSeq(boardFile.getBfSeq())
                        .regDate(now)
                        .build());

        asset.setMasterBucket(storageService.getBucket());
        asset.setMasterPath(boardFile.getBfPath());
        asset.setMimeType(normalizedMimeType != null ? normalizedMimeType : guessMimeType(extension));
        asset.setOptimizationStatus(STATUS_PENDING);
        asset.setLastError(null);
        asset.setUpdDate(now);

        BoardImageAsset savedAsset = boardImageAssetRepository.save(asset);

        BoardImageJob job = BoardImageJob.builder()
                .biaSeq(savedAsset.getBiaSeq())
                .jobType("OPTIMIZE")
                .status(STATUS_PENDING)
                .attemptCount(0)
                .nextRunAt(now)
                .lastError(null)
                .regDate(now)
                .updDate(now)
                .build();
        boardImageJobRepository.save(job);
    }

    public Map<Long, OptimizationSnapshot> getOptimizationSnapshotsByFileIds(Collection<Long> fileIds) {
        List<Long> normalizedFileIds = normalizeIds(fileIds);
        if (normalizedFileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BoardImageAsset> assets = boardImageAssetRepository.findByBfSeqIn(normalizedFileIds);
        if (assets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, BoardImageAsset> assetByFileId = assets.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BoardImageAsset::getBfSeq, asset -> asset, (left, right) -> right));

        List<Long> assetIds = assets.stream()
                .map(BoardImageAsset::getBiaSeq)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<BoardImageVariant>> variantsByAssetId = boardImageVariantRepository.findByBiaSeqIn(assetIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(BoardImageVariant::getBiaSeq));

        Map<Long, OptimizationSnapshot> snapshots = new HashMap<>();
        for (Long fileId : normalizedFileIds) {
            BoardImageAsset asset = assetByFileId.get(fileId);
            if (asset == null) {
                continue;
            }
            List<BoardImageVariant> variants = variantsByAssetId.getOrDefault(asset.getBiaSeq(), Collections.emptyList());
            BoardFileDto.ImageVariants imageVariants = buildSignedImageVariants(asset, variants);
            snapshots.put(fileId, new OptimizationSnapshot(asset.getMimeType(), asset.getOptimizationStatus(), imageVariants));
        }

        return snapshots;
    }

    public Map<Long, String> getPreferredThumbnailUrlsByFileIds(Collection<Long> fileIds) {
        Map<Long, OptimizationSnapshot> snapshots = getOptimizationSnapshotsByFileIds(fileIds);
        if (snapshots.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, OptimizationSnapshot> entry : snapshots.entrySet()) {
            String preferredUrl = pickPreferredThumbnailUrl(entry.getValue());
            if (preferredUrl != null) {
                result.put(entry.getKey(), preferredUrl);
            }
        }
        return result;
    }

    public Map<Long, BoardFileDto.ImageVariantSet> getPreferredThumbnailVariantSetsByFileIds(Collection<Long> fileIds) {
        Map<Long, OptimizationSnapshot> snapshots = getOptimizationSnapshotsByFileIds(fileIds);
        if (snapshots.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, BoardFileDto.ImageVariantSet> result = new HashMap<>();
        for (Map.Entry<Long, OptimizationSnapshot> entry : snapshots.entrySet()) {
            BoardFileDto.ImageVariantSet preferredSet = pickPreferredThumbnailVariantSet(entry.getValue());
            if (preferredSet != null) {
                result.put(entry.getKey(), preferredSet);
            }
        }
        return result;
    }

    public void applyOptimizationFields(BoardFileDto dto, BoardFile file) {
        if (dto == null || file == null || file.getBfSeq() == null) {
            return;
        }
        OptimizationSnapshot snapshot = getOptimizationSnapshotsByFileIds(List.of(file.getBfSeq())).get(file.getBfSeq());
        applyOptimizationFields(dto, file, snapshot);
    }

    public void applyOptimizationFields(BoardFileDto dto, BoardFile file, Map<Long, OptimizationSnapshot> snapshotsByFileId) {
        if (dto == null || file == null) {
            return;
        }
        OptimizationSnapshot snapshot = snapshotsByFileId == null ? null : snapshotsByFileId.get(file.getBfSeq());
        applyOptimizationFields(dto, file, snapshot);
    }

    public void applyOptimizationFields(BoardFileDto dto, BoardFile file, OptimizationSnapshot snapshot) {
        if (dto == null || file == null) {
            return;
        }

        String extension = normalizeExtension(file.getBfExtension());
        dto.setIsImage(LEGACY_IMAGE_EXTENSIONS.contains(extension));
        dto.setContentType(snapshot != null && hasText(snapshot.mimeType())
                ? snapshot.mimeType()
                : guessMimeType(extension));
        dto.setImageOptimizationStatus(snapshot == null ? null : snapshot.optimizationStatus());
        dto.setImageVariants(snapshot == null ? null : snapshot.imageVariants());
    }

    private BoardFileDto.ImageVariants buildSignedImageVariants(BoardImageAsset asset, List<BoardImageVariant> variants) {
        if (asset == null || variants == null || variants.isEmpty()) {
            return null;
        }

        Map<String, List<BoardImageVariant>> byRole = variants.stream()
                .filter(Objects::nonNull)
                .filter(variant -> hasText(variant.getVariantRole()))
                .collect(Collectors.groupingBy(variant -> variant.getVariantRole().toLowerCase(Locale.ROOT)));

        BoardFileDto.ImageVariantSet thumb = buildVariantSet(asset, byRole.get("thumb"));
        BoardFileDto.ImageVariantSet detail = buildVariantSet(asset, byRole.get("detail"));
        BoardFileDto.ImageVariantSet poster = buildVariantSet(asset, byRole.get("poster"));

        if (thumb == null && detail == null && poster == null) {
            return null;
        }

        return BoardFileDto.ImageVariants.builder()
                .thumb(thumb)
                .detail(detail)
                .poster(poster)
                .build();
    }

    private BoardFileDto.ImageVariantSet buildVariantSet(BoardImageAsset asset, List<BoardImageVariant> variants) {
        if (asset == null || variants == null || variants.isEmpty()) {
            return null;
        }

        String bucket = hasText(asset.getMasterBucket()) ? asset.getMasterBucket() : storageService.getBucket();
        BoardFileDto.ImageVariantSet.ImageVariantSetBuilder builder = BoardFileDto.ImageVariantSet.builder();

        Integer width = null;
        Integer height = null;

        for (BoardImageVariant variant : variants) {
            if (variant == null || !hasText(variant.getFilePath())) {
                continue;
            }
            String format = normalizeFormat(variant.getFormat());
            String signedUrl = getSignedUrlSafe(variant.getFilePath(), bucket);
            if (!hasText(signedUrl)) {
                continue;
            }

            if (width == null && variant.getWidth() != null) {
                width = variant.getWidth();
            }
            if (height == null && variant.getHeight() != null) {
                height = variant.getHeight();
            }

            switch (format) {
                case "avif" -> builder.avifUrl(signedUrl);
                case "webp" -> builder.webpUrl(signedUrl);
                case "jpeg", "jpg" -> builder.jpegUrl(signedUrl);
                case "png" -> builder.pngUrl(signedUrl);
                default -> {
                    // ignore unsupported format string
                }
            }
        }

        BoardFileDto.ImageVariantSet built = builder
                .width(width)
                .height(height)
                .build();

        if (!hasText(built.getAvifUrl())
                && !hasText(built.getWebpUrl())
                && !hasText(built.getJpegUrl())
                && !hasText(built.getPngUrl())) {
            return null;
        }
        return built;
    }

    private String pickPreferredThumbnailUrl(OptimizationSnapshot snapshot) {
        return pickPreferredRenderableUrl(pickPreferredThumbnailVariantSet(snapshot));
    }

    private BoardFileDto.ImageVariantSet pickPreferredThumbnailVariantSet(OptimizationSnapshot snapshot) {
        if (snapshot == null || snapshot.imageVariants() == null) {
            return null;
        }
        BoardFileDto.ImageVariantSet thumb = snapshot.imageVariants().getThumb();
        if (hasRenderableUrl(thumb)) {
            return thumb;
        }

        BoardFileDto.ImageVariantSet poster = snapshot.imageVariants().getPoster();
        if (hasRenderableUrl(poster)) {
            return poster;
        }
        return null;
    }

    private String pickPreferredRenderableUrl(BoardFileDto.ImageVariantSet variantSet) {
        if (variantSet == null) {
            return null;
        }
        if (hasText(variantSet.getWebpUrl())) return variantSet.getWebpUrl();
        if (hasText(variantSet.getJpegUrl())) return variantSet.getJpegUrl();
        if (hasText(variantSet.getPngUrl())) return variantSet.getPngUrl();
        if (hasText(variantSet.getAvifUrl())) return variantSet.getAvifUrl();
        return null;
    }

    private boolean hasRenderableUrl(BoardFileDto.ImageVariantSet variantSet) {
        return pickPreferredRenderableUrl(variantSet) != null;
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFormat(String format) {
        if (format == null) {
            return "";
        }
        return format.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String mimeType) {
        if (!hasText(mimeType)) {
            return null;
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized) || "image/pjpeg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private String guessMimeType(String extension) {
        return switch (normalizeExtension(extension)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> null;
        };
    }

    private String getSignedUrlSafe(String path, String bucket) {
        if (!hasText(path)) {
            return null;
        }
        try {
            if (hasText(bucket) && !Objects.equals(bucket, storageService.getBucket())) {
                return storageService.getSignedUrl(path, SIGNED_URL_EXPIRE_SECONDS, bucket);
            }
            return storageService.getSignedUrl(path, SIGNED_URL_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to create signed URL for variant path: {}", path, e);
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
