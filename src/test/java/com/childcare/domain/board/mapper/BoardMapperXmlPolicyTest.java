package com.childcare.domain.board.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardMapperXmlPolicyTest {

    @Test
    void getCommentsShouldKeepOnlyDeletedParentsWithActiveReplies() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/mapper/board/BoardMapper.xml")) {
            assertNotNull(inputStream);

            String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(xml.contains("c.parent_seq IS NULL"));
            assertTrue(xml.contains("c.delete_yn = 'Y'"));
            assertTrue(xml.contains("FROM board_comment child"));
            assertTrue(xml.contains("child.parent_seq = c.bc_seq"));
            assertTrue(xml.contains("child.delete_yn IS NULL OR child.delete_yn != 'Y'"));
        }
    }
}
