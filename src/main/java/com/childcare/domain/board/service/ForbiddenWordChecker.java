package com.childcare.domain.board.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ForbiddenWordChecker {

    // 금지어 목록
    private static final List<String> FORBIDDEN_WORDS = Arrays.asList(
            // SQL Injection 관련
            "drop table", "drop database", "truncate table",
            "union select", "union all select",
            "exec xp_", "execute xp_", "xp_cmdshell",
            "sp_executesql", "0x", "/*", "*/", "--",
            "sysobjects", "syscolumns", "information_schema",

            // XSS 관련
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "onclick=", "onmouseover=", "onfocus=", "onblur=",

            // 욕설 등 금지어
            "원하는 문구를 추가하시면 됩니다."
    );

    // 대소문자 무시 패턴 생성
    private static final List<Pattern> FORBIDDEN_PATTERNS;

    static {
        FORBIDDEN_PATTERNS = FORBIDDEN_WORDS.stream()
                .map(word -> Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE))
                .toList();
    }

    /**
     * 금지어 포함 여부 확인
     * @param text 검사할 텍스트
     * @return 금지어가 포함되어 있으면 true
     */
    public boolean containsForbiddenWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        // 공백 제거한 텍스트도 검사 (우회 방지)
        String normalizedText = text.replaceAll("\\s+", "");

        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(text).find() || pattern.matcher(normalizedText).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 제목과 내용 모두 검사
     * @param title 제목
     * @param content 내용
     * @return 금지어가 포함되어 있으면 true
     */
    public boolean containsForbiddenWord(String title, String content) {
        return containsForbiddenWord(title) || containsForbiddenWord(content);
    }
}
