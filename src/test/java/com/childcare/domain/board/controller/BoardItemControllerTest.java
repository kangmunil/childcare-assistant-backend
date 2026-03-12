package com.childcare.domain.board.controller;

import com.childcare.domain.board.dto.BoardItemDto;
import com.childcare.domain.board.dto.BoardItemRequest;
import com.childcare.domain.board.dto.BoardUrgentResolveRequest;
import com.childcare.domain.board.dto.BoardSearchRequest;
import com.childcare.domain.board.service.BoardItemService;
import com.childcare.global.config.JwtAuthenticationFilter;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.BoardException;
import com.childcare.global.exception.BoardException.BoardErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardItemService boardItemService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID memberId;

    @BeforeEach
    void setUpSecurityContext() {
        memberId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getItemListBySlug_forwardsLocationScopeAndSort() throws Exception {
        when(boardItemService.getItemListBySlug(any(UUID.class), eq("community"), any(BoardSearchRequest.class)))
                .thenReturn(ApiResponse.success("목록 조회 성공", Map.of(
                        "items", List.of(),
                        "totalElements", 0,
                        "totalPages", 0,
                        "currentPage", 0,
                        "size", 20
                )));

        mockMvc.perform(get("/boards/community/items")
                        .param("locationScope", "all")
                        .param("sort", "popular")
                        .param("category", "daily")
                        .param("page", "1")
                        .param("size", "10")
                        .param("includeHighlights", "false")
                        .param("urgentSlot", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        ArgumentCaptor<BoardSearchRequest> captor = ArgumentCaptor.forClass(BoardSearchRequest.class);
        verify(boardItemService).getItemListBySlug(eq(memberId), eq("community"), captor.capture());
        BoardSearchRequest request = captor.getValue();
        assertEquals("all", request.getLocationScope());
        assertEquals("popular", request.getSort());
        assertEquals("daily", request.getCategory());
        assertEquals(1, request.getPage());
        assertEquals(10, request.getSize());
        assertEquals(false, request.isIncludeHighlights());
        assertEquals(true, request.isUrgentSlot());
    }

    @Test
    void getItemListBySlug_includesThumbnailVariantFieldsInResponse() throws Exception {
        Map<String, Object> item = Map.of(
                "id", 10,
                "title", "썸네일 필드 계약",
                "hasFile", true,
                "thumbnailUrl", "https://example.com/thumb.webp",
                "thumbnailAvifUrl", "https://example.com/thumb.avif",
                "thumbnailWebpUrl", "https://example.com/thumb.webp",
                "thumbnailJpegUrl", "https://example.com/thumb.jpeg",
                "thumbnailPngUrl", "https://example.com/thumb.png",
                "thumbnailWidth", 320,
                "thumbnailHeight", 240
        );

        when(boardItemService.getItemListBySlug(any(UUID.class), eq("community"), any(BoardSearchRequest.class)))
                .thenReturn(ApiResponse.success("목록 조회 성공", Map.of(
                        "items", List.of(item),
                        "totalElements", 1,
                        "totalPages", 1,
                        "currentPage", 0,
                        "size", 20
                )));

        mockMvc.perform(get("/boards/community/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value("https://example.com/thumb.webp"))
                .andExpect(jsonPath("$.data.items[0].thumbnailAvifUrl").value("https://example.com/thumb.avif"))
                .andExpect(jsonPath("$.data.items[0].thumbnailWebpUrl").value("https://example.com/thumb.webp"))
                .andExpect(jsonPath("$.data.items[0].thumbnailJpegUrl").value("https://example.com/thumb.jpeg"))
                .andExpect(jsonPath("$.data.items[0].thumbnailPngUrl").value("https://example.com/thumb.png"))
                .andExpect(jsonPath("$.data.items[0].thumbnailWidth").value(320))
                .andExpect(jsonPath("$.data.items[0].thumbnailHeight").value(240));
    }

    @Test
    void createItemBySlug_forwardsPostScopeAndPlaceFields() throws Exception {
        BoardItemDto responseDto = BoardItemDto.builder()
                .id(10L)
                .postScope("neighbor")
                .category("local_review")
                .placeName("테스트 소아과")
                .build();
        when(boardItemService.createItemBySlug(any(UUID.class), eq("community"), any(BoardItemRequest.class)))
                .thenReturn(ApiResponse.success("게시글 작성 성공", responseDto));

        String payload = """
                {
                  "title": "동네후기 테스트",
                  "content": "위치와 장소 정책 검증",
                  "category": "local_review",
                  "postScope": "neighbor",
                  "placeName": "테스트 소아과",
                  "placeAddress": "서울 강남구 테스트로 1",
                  "placeLat": 37.498,
                  "placeLng": 127.027
                }
                """;

        mockMvc.perform(post("/boards/community/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.postScope").value("neighbor"))
                .andExpect(jsonPath("$.data.category").value("local_review"));

        ArgumentCaptor<BoardItemRequest> captor = ArgumentCaptor.forClass(BoardItemRequest.class);
        verify(boardItemService).createItemBySlug(eq(memberId), eq("community"), captor.capture());
        BoardItemRequest request = captor.getValue();
        assertEquals("neighbor", request.getPostScope());
        assertEquals("local_review", request.getCategory());
        assertEquals("테스트 소아과", request.getPlaceName());
        assertEquals("서울 강남구 테스트로 1", request.getPlaceAddress());
    }

    @Test
    void createItemBySlug_whenServiceThrowsBoard031_returnsBadRequest() throws Exception {
        when(boardItemService.createItemBySlug(any(UUID.class), anyString(), any(BoardItemRequest.class)))
                .thenThrow(new BoardException(BoardErrorCode.ITEM_PLACE_REQUIRED));

        String payload = """
                {
                  "title": "동네후기 테스트",
                  "content": "장소 누락",
                  "category": "local_review",
                  "postScope": "neighbor"
                }
                """;

        mockMvc.perform(post("/boards/community/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BOARD_031"));
    }

    @Test
    void updateItemBySlug_whenServiceThrowsBoard032_returnsBadRequest() throws Exception {
        when(boardItemService.updateItemBySlug(any(UUID.class), anyString(), eq(10L), any(BoardItemRequest.class)))
                .thenThrow(new BoardException(BoardErrorCode.ITEM_SCOPE_CATEGORY_INVALID));

        String payload = """
                {
                  "title": "수정 제목",
                  "content": "수정 내용",
                  "category": "info_share",
                  "postScope": "all"
                }
                """;

        mockMvc.perform(put("/boards/community/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BOARD_032"));
    }

    @Test
    void resolveUrgentItemBySlug_forwardsResolvedFlag() throws Exception {
        BoardItemDto responseDto = BoardItemDto.builder()
                .id(10L)
                .category("urgent")
                .urgentResolved(true)
                .build();
        when(boardItemService.resolveUrgentItemBySlug(
                any(UUID.class),
                eq("community"),
                eq(10L),
                any(BoardUrgentResolveRequest.class)))
                .thenReturn(ApiResponse.success("긴급/SOS 해결 처리 완료", responseDto));

        mockMvc.perform(put("/boards/community/items/10/urgent-resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resolved": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.urgentResolved").value(true));

        ArgumentCaptor<BoardUrgentResolveRequest> captor = ArgumentCaptor.forClass(BoardUrgentResolveRequest.class);
        verify(boardItemService).resolveUrgentItemBySlug(eq(memberId), eq("community"), eq(10L), captor.capture());
        assertEquals(true, captor.getValue().isResolved());
    }

    @Test
    void resolveUrgentItemBySlug_whenServiceThrowsBoard033_returnsBadRequest() throws Exception {
        when(boardItemService.resolveUrgentItemBySlug(
                any(UUID.class),
                anyString(),
                eq(10L),
                any(BoardUrgentResolveRequest.class)))
                .thenThrow(new BoardException(BoardErrorCode.ITEM_URGENT_RESOLVE_INVALID));

        mockMvc.perform(put("/boards/community/items/10/urgent-resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resolved": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BOARD_033"));
    }

    @Test
    void getItemListBySlug_reservedSlugReturnsBoard029() throws Exception {
        mockMvc.perform(get("/boards/admin/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BOARD_029"));

        verify(boardItemService, never()).getItemListBySlug(any(UUID.class), anyString(), any(BoardSearchRequest.class));
    }

    @Test
    void getItemListBySlug_withoutAuthenticationReturns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/boards/community/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verify(boardItemService, never()).getItemListBySlug(any(UUID.class), anyString(), any(BoardSearchRequest.class));
    }
}
