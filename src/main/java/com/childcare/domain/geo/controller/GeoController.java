package com.childcare.domain.geo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Kakao Local API Proxy
 * - 프론트엔드에서 직접 Kakao API를 호출하면 API Key가 노출되므로
 * - 백엔드를 통해 프록시하여 보안 유지
 */
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
@Slf4j
public class GeoController {

    @Value("${kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 좌표를 주소로 변환 (Reverse Geocoding)
     * 
     * @param lat 위도
     * @param lng 경도
     * @return Kakao API 응답 (주소 정보)
     */
    @GetMapping("/reverse")
    public ResponseEntity<?> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {

        log.info("Reverse geocoding request: lat={}, lng={}", lat, lng);

        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            log.error("Kakao API Key is missing! Check application.yml or environment variables.");
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "code", "GEO_CONFIG_MISSING",
                            "error", "Kakao REST API key not configured"));
        }

        try {
            log.info("Calling Kakao Local API for reverse geocoding");

            String url = String.format(
                    "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=%f&y=%f",
                    lng, lat // Kakao API: x = 경도(lng), y = 위도(lat)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("Kakao Local API authentication failed: status={}, body={}",
                        e.getStatusCode().value(), e.getResponseBodyAsString());
                return ResponseEntity.status(502)
                        .body(Map.of(
                                "code", "GEO_UPSTREAM_AUTH",
                                "error", "Kakao Local API authentication failed"));
            }

            log.error("Kakao Local API returned client error: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(502)
                    .body(Map.of(
                            "code", "GEO_UPSTREAM_FAILED",
                            "error", "Kakao Local API request failed"));
        } catch (RestClientException e) {
            log.error("Kakao Local API request failed", e);
            return ResponseEntity.status(502)
                    .body(Map.of(
                            "code", "GEO_UPSTREAM_FAILED",
                            "error", "Kakao Local API request failed"));
        }
    }
}
