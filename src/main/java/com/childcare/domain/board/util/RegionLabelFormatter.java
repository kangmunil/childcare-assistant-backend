package com.childcare.domain.board.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RegionLabelFormatter {

    private RegionLabelFormatter() {
    }

    public static String normalize(String regionName) {
        if (regionName == null) {
            return null;
        }

        String normalized = regionName.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    public static boolean isSameNeighborhood(String a, String b) {
        String normalizedA = normalize(a);
        String normalizedB = normalize(b);
        if (normalizedA == null || normalizedB == null) {
            return false;
        }
        return normalizedA.equals(normalizedB);
    }

    public static String toDongLabel(String regionName) {
        String normalized = normalize(regionName);
        if (normalized == null) {
            return null;
        }

        List<String> tokens = tokenize(normalized);
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (endsWithNeighborhoodUnit(token)) {
                return token;
            }
        }
        return normalized;
    }

    public static String toGuDongLabel(String regionName) {
        String normalized = normalize(regionName);
        if (normalized == null) {
            return null;
        }

        List<String> tokens = tokenize(normalized);
        String dong = null;
        String gu = null;

        for (String token : tokens) {
            if (endsWithNeighborhoodUnit(token)) {
                dong = token;
            }
            if (token.endsWith("구")) {
                gu = token;
            }
        }

        if (dong != null && gu != null) {
            return gu.equals(dong) ? dong : gu + " " + dong;
        }
        if (dong != null) {
            return dong;
        }
        return normalized;
    }

    private static List<String> tokenize(String normalized) {
        String[] split = normalized.split(" ");
        List<String> tokens = new ArrayList<>(split.length);
        for (String token : split) {
            String value = token == null ? "" : token.trim();
            if (!value.isBlank()) {
                tokens.add(value);
            }
        }
        return tokens;
    }

    private static boolean endsWithNeighborhoodUnit(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.endsWith("동") || lower.endsWith("읍") || lower.endsWith("면") || lower.endsWith("리");
    }
}
