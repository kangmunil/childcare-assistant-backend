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
}