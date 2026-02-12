package com.childcare.domain.child.controller;

import com.childcare.domain.child.dto.ChildImageDto;
import com.childcare.domain.child.service.ChildImageService;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/children/{childId}/images")
@RequiredArgsConstructor
@Slf4j
public class ChildImageController {

    private final ChildImageService childImageService;

    /**
     * 자녀 프로필 이미지 업로드
     * POST /children/{childId}/images
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChildImageDto>> uploadImage(
            @PathVariable Long childId,
            @RequestParam("file") MultipartFile file) {
        UUID memberId = getMemberId();
        log.info("Upload image for child: {}, member: {}", childId, memberId);

        ApiResponse<ChildImageDto> response = childImageService.uploadImage(memberId, childId, file);
        return ResponseEntity.ok(response);
    }

    /**
     * 자녀 프로필 이미지 삭제
     * DELETE /children/{childId}/images
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long childId) {
        UUID memberId = getMemberId();
        log.info("Delete image for child: {}, member: {}", childId, memberId);

        ApiResponse<Void> response = childImageService.deleteImage(memberId, childId);
        return ResponseEntity.ok(response);
    }

    private UUID getMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }
}
