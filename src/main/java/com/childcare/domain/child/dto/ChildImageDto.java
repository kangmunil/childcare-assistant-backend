package com.childcare.domain.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildImageDto {
    private Long id;
    private Long childId;
    private String orgFilename;
    private String fileName;
    private String filePath;
    private String extension;
    private Integer fileSize;
    private LocalDateTime regDate;
    private String downloadUrl;
}
