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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
            Map<String, Object> addressResponse = callKakaoLocalApi("coord2address", lat, lng);
            Map<String, Object> regionResponse = new HashMap<>();

            // 지역 코드 API 실패 시에도 주소 결과는 반환한다.
            try {
                regionResponse = callKakaoLocalApi("coord2regioncode", lat, lng);
            } catch (RestClientException e) {
                log.warn("coord2regioncode failed, fallback to address-only response: {}", e.getMessage());
            }

            List<Map<String, Object>> addressDocs = extractDocuments(addressResponse);
            List<Map<String, Object>> regionDocs = extractDocuments(regionResponse);

            Map<String, Object> legalRegion = findRegionByType(regionDocs, "B");
            Map<String, Object> adminRegion = findRegionByType(regionDocs, "H");
            Map<String, Object> firstAddressDoc = addressDocs.isEmpty() ? Map.of() : addressDocs.get(0);

            Map<String, Object> address = asMap(firstAddressDoc.get("address"));
            Map<String, Object> roadAddress = asMap(firstAddressDoc.get("road_address"));

            String legalRegionName = stringValue(legalRegion.get("address_name"));
            String adminRegionName = stringValue(adminRegion.get("address_name"));

            String fallbackRegionName = firstNonBlank(
                    stringValue(address.get("region_3depth_name")),
                    stringValue(roadAddress.get("region_3depth_name")),
                    stringValue(address.get("region_2depth_name")));

            String preferredRegionName = firstNonBlank(legalRegionName, adminRegionName, fallbackRegionName);
            String fullAddress = firstNonBlank(
                    stringValue(roadAddress.get("address_name")),
                    stringValue(address.get("address_name")));

            Map<String, Object> mergedResponse = new HashMap<>();
            mergedResponse.put("documents", addressDocs);
            mergedResponse.put("meta", addressResponse.get("meta"));
            mergedResponse.put("regionDocuments", regionDocs);
            mergedResponse.put("legalRegionCode", stringValue(legalRegion.get("code")));
            mergedResponse.put("legalRegionName", legalRegionName);
            mergedResponse.put("adminRegionCode", stringValue(adminRegion.get("code")));
            mergedResponse.put("adminRegionName", adminRegionName);
            mergedResponse.put("preferredRegionName", preferredRegionName);
            mergedResponse.put("fullAddress", fullAddress);

            return ResponseEntity.ok(mergedResponse);

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

    private Map<String, Object> callKakaoLocalApi(String endpoint, double lat, double lng) {
        String url = String.format(
                Locale.US,
                "https://dapi.kakao.com/v2/local/geo/%s.json?x=%.8f&y=%.8f&input_coord=WGS84",
                endpoint,
                lng,
                lat);

        return executeKakaoRequest(url);
    }

    /**
     * 카카오 장소 검색 API (키워드로 검색) Proxy
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPlaces(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius) {

        log.info("Place search request: query={}, lat={}, lng={}, radius={}", query, lat, lng, radius);

        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "GEO_CONFIG_MISSING", "error", "Kakao REST API key not configured"));
        }

        try {
            StringBuilder urlBuilder = new StringBuilder("https://dapi.kakao.com/v2/local/search/keyword.json");
            urlBuilder.append("?query=")
                    .append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
            urlBuilder.append("&page=").append(page);
            urlBuilder.append("&size=15"); // default sizes

            if (lat != null && lng != null) {
                urlBuilder.append("&y=").append(lat);
                urlBuilder.append("&x=").append(lng);
                if (radius != null) {
                    urlBuilder.append("&radius=").append(radius);
                }
            }

            Map<String, Object> response = executeKakaoRequest(urlBuilder.toString());
            return ResponseEntity.ok(response);

        } catch (HttpClientErrorException e) {
            log.error("Kakao Local API search failed: status={}, body={}", e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return ResponseEntity.status(502).body(Map.of("code", "GEO_UPSTREAM_FAILED", "error", "Kakao API failed"));
        } catch (Exception e) {
            log.error("Kakao Local API search failed", e);
            return ResponseEntity.status(502)
                    .body(Map.of("code", "GEO_UPSTREAM_FAILED", "error", "Internal server error"));
        }
    }

    private Map<String, Object> executeKakaoRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class);
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private List<Map<String, Object>> extractDocuments(Map<String, Object> payload) {
        if (payload == null) {
            return List.of();
        }
        Object documents = payload.get("documents");
        if (!(documents instanceof List<?> rawList)) {
            return List.of();
        }
        return rawList.stream()
                .filter(Map.class::isInstance)
                .map(item -> asMap(item))
                .toList();
    }

    private Map<String, Object> findRegionByType(List<Map<String, Object>> documents, String regionType) {
        if (documents == null || documents.isEmpty()) {
            return Map.of();
        }

        for (Map<String, Object> document : documents) {
            if (regionType.equals(stringValue(document.get("region_type")))) {
                return document;
            }
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            return (Map<String, Object>) rawMap;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
