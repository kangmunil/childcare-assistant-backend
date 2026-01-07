package com.childcare.global.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

/**
 * Supabase Storage API 연동 서비스
 */
@Service
@Slf4j
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String bucket;
    private final String supabaseUrl;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-key}") String serviceKey,
            @Value("${supabase.storage.bucket}") String bucket) {

        this.bucket = bucket;
        this.supabaseUrl = supabaseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .defaultHeader("apikey", serviceKey)
                .build();
    }

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param path 저장 경로 (예: board/free/20251227/uuid.jpg)
     * @return 저장된 파일의 전체 경로
     */
    public String uploadFile(MultipartFile file, String path) {
        try {
            log.info("Uploading file to Supabase Storage: {}", path);

            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            webClient.post()
                    .uri("/object/{bucket}/{path}", bucket, path)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("x-upsert", "true")
                    .bodyValue(fileBytes)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("File uploaded successfully: {}", path);
            return path;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", path, e);
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 삭제
     * @param path 삭제할 파일 경로
     */
    public void deleteFile(String path) {
        log.info("Deleting file from Supabase Storage: {}", path);

        try {
            webClient.delete()
                    .uri("/object/{bucket}/{path}", bucket, path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("File deleted successfully: {}", path);

        } catch (Exception e) {
            log.warn("Failed to delete file (may not exist): {}", path, e);
        }
    }

    /**
     * 공개 URL 생성 (Public 버킷용)
     * @param path 파일 경로
     * @return 공개 URL
     */
    public String getPublicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    /**
     * 서명된 URL 생성 (Private 버킷용)
     * @param path 파일 경로
     * @param expiresIn 만료 시간 (초)
     * @return 서명된 다운로드 URL
     */
    public String getSignedUrl(String path, int expiresIn) {
        log.info("Creating signed URL for: {}, expires in: {}s", path, expiresIn);

        try {
            String response = webClient.post()
                    .uri("/object/sign/{bucket}/{path}", bucket, path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"expiresIn\": " + expiresIn + "}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 응답에서 signedURL 추출 (간단한 파싱)
            // 응답 형식: {"signedURL": "/object/sign/bucket/path?token=xxx"}
            if (response != null && response.contains("signedURL")) {
                int start = response.indexOf("signedURL") + 13;
                int end = response.indexOf("\"", start);
                String signedPath = response.substring(start, end);
                return supabaseUrl + "/storage/v1" + signedPath;
            }

            throw new RuntimeException("서명된 URL 생성 실패");

        } catch (Exception e) {
            log.error("Failed to create signed URL: {}", path, e);
            throw new RuntimeException("서명된 URL 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 버킷명 반환
     */
    public String getBucket() {
        return bucket;
    }
}