package com.childcare.domain.board.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardCommentDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAuthorFlagAsIsAuthor() {
        BoardCommentDto dto = BoardCommentDto.builder()
                .id(1L)
                .liked(false)
                .isAuthor(true)
                .build();

        JsonNode json = objectMapper.valueToTree(dto);

        assertTrue(json.has("isAuthor"));
        assertTrue(json.get("isAuthor").asBoolean());
        assertFalse(json.has("author"));
    }
}
