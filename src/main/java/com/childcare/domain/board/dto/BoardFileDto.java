package com.childcare.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFileDto {
    private Long id;
    private Long itemId;
    private String orgFilename;
    private String fileName;
    private String filePath;
    private String extension;
    private Integer fileSize;
    private LocalDateTime regDate;
    private String downloadUrl;  // Supabase Storage 다운로드 URL
    private String contentType;
    private Boolean isImage;
    private String imageOptimizationStatus;
    private ImageVariants imageVariants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageVariants {
        private ImageVariantSet thumb;
        private ImageVariantSet detail;
        private ImageVariantSet poster;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageVariantSet {
        private String avifUrl;
        private String webpUrl;
        private String jpegUrl;
        private String pngUrl;
        private Integer width;
        private Integer height;
    }
}
