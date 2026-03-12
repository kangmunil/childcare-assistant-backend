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
     * 버킷이 없으면 생성
     * @param bucketName 생성할 버킷명
     * @param isPublic 공개 여부
     */
    public void createBucketIfNotExists(String bucketName, boolean isPublic) {
        try {
            // 버킷 존재 여부 확인
            webClient.get()
                    .uri("/bucket/{bucketName}", bucketName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Bucket already exists: {}", bucketName);

        } catch (Exception e) {
            // 버킷이 없으면 생성
            log.info("Creating bucket: {}", bucketName);
            try {
                String body = String.format(
                        "{\"id\": \"%s\", \"name\": \"%s\", \"public\": %s}",
                        bucketName, bucketName, isPublic);

                webClient.post()
                        .uri("/bucket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("Bucket created successfully: {}", bucketName);

            } catch (Exception ex) {
                log.error("Failed to create bucket: {}", bucketName, ex);
            }
        }
    }

    // ==================== 기본 버킷 메서드 (기존 호환) ====================

    /**
     * 파일 업로드 (기본 버킷)
     */
    public String uploadFile(MultipartFile file, String path) {
        return uploadFile(file, path, this.bucket);
    }

    /**
     * 파일 삭제 (기본 버킷)
     */
    public void deleteFile(String path) {
        deleteFile(path, this.bucket);
    }

    /**
     * 공개 URL 생성 (기본 버킷)
     */
    public String getPublicUrl(String path) {
        return getPublicUrl(path, this.bucket);
    }

    /**
     * 서명된 URL 생성 (기본 버킷)
     */
    public String getSignedUrl(String path, int expiresIn) {
        return getSignedUrl(path, expiresIn, this.bucket);
    }

    // ==================== 버킷 지정 메서드 ====================

    /**
     * 파일 업로드 (버킷 지정)
     * @param file 업로드할 파일
     * @param path 저장 경로
     * @param targetBucket 대상 버킷명
     * @return 저장된 파일의 경로
     */
    public String uploadFile(MultipartFile file, String path, String targetBucket) {
        try {
            log.info("Uploading file to Supabase Storage [{}]: {}", targetBucket, path);

            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            webClient.post()
                    .uri("/object/" + targetBucket + "/" + path)
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
     * 파일 삭제 (버킷 지정)
     * @param path 삭제할 파일 경로
     * @param targetBucket 대상 버킷명
     */
    public void deleteFile(String path, String targetBucket) {
        log.info("Deleting file from Supabase Storage [{}]: {}", targetBucket, path);

        try {
            webClient.delete()
                    .uri("/object/" + targetBucket + "/" + path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("File deleted successfully: {}", path);

        } catch (Exception e) {
            log.warn("Failed to delete file (may not exist): {}", path, e);
        }
    }

    /**
     * 공개 URL 생성 (버킷 지정)
     * @param path 파일 경로
     * @param targetBucket 대상 버킷명
     * @return 공개 URL
     */
    public String getPublicUrl(String path, String targetBucket) {
        return supabaseUrl + "/storage/v1/object/public/" + targetBucket + "/" + path;
    }

    /**
     * 서명된 URL 생성 (버킷 지정)
     * @param path 파일 경로
     * @param expiresIn 만료 시간 (초)
     * @param targetBucket 대상 버킷명
     * @return 서명된 다운로드 URL
     */
    public String getSignedUrl(String path, int expiresIn, String targetBucket) {
        log.info("Creating signed URL for [{}]: {}, expires in: {}s", targetBucket, path, expiresIn);

        try {
            String response = webClient.post()
                    .uri("/object/sign/" + targetBucket + "/" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"expiresIn\": " + expiresIn + "}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 응답에서 signedURL 추출
            // 응답 형식: {"signedURL":"/object/sign/bucket/path?token=xxx"}
            if (response != null && response.contains("signedURL")) {
                int keyStart = response.indexOf("signedURL");
                int valueStart = response.indexOf("\"", keyStart + 9 + 1) + 1;
                int valueEnd = response.indexOf("\"", valueStart);
                String signedPath = response.substring(valueStart, valueEnd);
                return supabaseUrl + "/storage/v1" + signedPath;
            }

            throw new RuntimeException("서명된 URL 생성 실패");

        } catch (Exception e) {
            log.error("Failed to create signed URL: {}", path, e);
            throw new RuntimeException("서명된 URL 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기본 버킷명 반환
     */
    public String getBucket() {
        return bucket;
    }
}
