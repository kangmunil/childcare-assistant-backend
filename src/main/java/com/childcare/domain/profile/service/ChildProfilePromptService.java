package com.childcare.domain.profile.service;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.growth.entity.ChildGrowHistory;
import com.childcare.domain.growth.mapper.GrowthHistoryMapper;
import com.childcare.global.exception.ChildAccessDeniedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChildProfilePromptService {

    private static final Pattern MEASUREMENT_VALUE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private final ChildMapper childMapper;
    private final GrowthHistoryMapper growthHistoryMapper;
    private final ChildProfileService childProfileService;

    public ResolvedProfileContext resolveProfileContext(UUID memberId, Long requestedChildId) {
        List<Child> readableChildren = childMapper.findActiveChildrenByMemberId(memberId);

        if (readableChildren.isEmpty() && requestedChildId != null) {
            throw new ChildAccessDeniedException(ChildAccessDeniedException.AccessErrorCode.NO_ACCESS);
        }

        if (readableChildren.isEmpty()) {
            return ResolvedProfileContext.empty();
        }

        Child selectedChild;
        if (requestedChildId != null) {
            selectedChild = readableChildren.stream()
                    .filter(child -> child.getChSeq().equals(requestedChildId))
                    .findFirst()
                    .orElse(null);
            if (selectedChild == null) {
                throw new ChildAccessDeniedException(ChildAccessDeniedException.AccessErrorCode.NO_ACCESS);
            }
        } else {
            selectedChild = readableChildren.get(0);
        }

        Long selectedChildId = selectedChild.getChSeq();
        String summaryText = childProfileService.getOrCreateSummaryText(selectedChildId);
        Map<String, Object> growthContext = buildGrowthContext(selectedChild);
        return new ResolvedProfileContext(selectedChildId, summaryText, growthContext);
    }

    private Map<String, Object> buildGrowthContext(Child child) {
        Optional<ChildGrowHistory> latestHistory = growthHistoryMapper.findLatestActiveHistoryByChildId(child.getChSeq());

        Double latestHistoryHeight = latestHistory.map(ChildGrowHistory::getHeight)
                .map(this::parsePositiveNumber)
                .orElse(null);
        Double latestHistoryWeight = latestHistory.map(ChildGrowHistory::getWeight)
                .map(this::parsePositiveNumber)
                .orElse(null);

        Double childHeight = parsePositiveNumber(child.getHeight());
        Double childWeight = parsePositiveNumber(child.getWeight());

        Double resolvedHeight = latestHistoryHeight != null ? latestHistoryHeight : childHeight;
        Double resolvedWeight = latestHistoryWeight != null ? latestHistoryWeight : childWeight;

        boolean useHistory = latestHistoryHeight != null || latestHistoryWeight != null;
        LocalDate measuredDate = useHistory
                ? latestHistory.map(ChildGrowHistory::getGhDate)
                .map(this::parseDate)
                .orElse(null)
                : null;

        if (measuredDate == null && useHistory) {
            measuredDate = latestHistory.map(ChildGrowHistory::getRegDate)
                    .map(java.time.LocalDateTime::toLocalDate)
                    .orElse(null);
        }

        int staleDays = measuredDate == null
                ? -1
                : Math.max(0, (int) ChronoUnit.DAYS.between(measuredDate, LocalDate.now()));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("gender", normalizeGender(child.getGender()));
        context.put("birth_date", normalizeBirthDate(child.getBirthDay()));
        context.put("height_cm", resolvedHeight);
        context.put("weight_kg", resolvedWeight);
        context.put("measured_date", measuredDate == null ? null : measuredDate.toString());
        context.put("stale_days", staleDays);
        context.put("data_source", useHistory ? "history" : "child_profile");

        List<String> missingFields = new ArrayList<>();
        if (context.get("gender") == null) {
            missingFields.add("성별");
        }
        if (context.get("birth_date") == null) {
            missingFields.add("생년월일");
        }
        if (context.get("height_cm") == null) {
            missingFields.add("키");
        }
        if (context.get("weight_kg") == null) {
            missingFields.add("몸무게");
        }

        if (!missingFields.isEmpty()) {
            log.warn("성장 컨텍스트 누락 항목 - childId={}, missing={}", child.getChSeq(), missingFields);
        }

        return context;
    }

    private String normalizeGender(String rawGender) {
        if (!StringUtils.hasText(rawGender)) {
            return null;
        }
        String normalized = rawGender.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "m", "남", "남아", "남성", "male", "1" -> "M";
            case "f", "여", "여아", "여성", "female", "2" -> "F";
            default -> null;
        };
    }

    private String normalizeBirthDate(String rawBirthDate) {
        LocalDate parsed = parseDate(rawBirthDate);
        return parsed == null ? null : parsed.toString();
    }

    private LocalDate parseDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }

        String normalized = rawDate.trim();
        if (normalized.length() > 10) {
            normalized = normalized.substring(0, 10);
        }

        String[] candidates = {
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy.MM.dd",
                "yyyyMMdd"
        };

        for (String pattern : candidates) {
            try {
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // ignore and continue
            }
        }

        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // ignore
        }

        return null;
    }

    private Double parsePositiveNumber(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        try {
            String normalized = rawValue.trim().replace(",", ".").replaceAll("\\s+", "");
            if (normalized.contains("-")) {
                return null;
            }
            Matcher matcher = MEASUREMENT_VALUE_PATTERN.matcher(normalized);
            if (!matcher.find()) {
                return null;
            }

            double parsed = Double.parseDouble(matcher.group(1));
            if (Double.isFinite(parsed) && parsed > 0) {
                return parsed;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Getter
    public static class ResolvedProfileContext {
        private final Long childId;
        private final String profileContext;
        private final Map<String, Object> growthContext;

        public ResolvedProfileContext(Long childId, String profileContext) {
            this(childId, profileContext, null);
        }

        public ResolvedProfileContext(Long childId, String profileContext, Map<String, Object> growthContext) {
            this.childId = childId;
            this.profileContext = profileContext;
            this.growthContext = growthContext == null
                    ? null
                    : Collections.unmodifiableMap(new LinkedHashMap<>(growthContext));
        }

        public static ResolvedProfileContext empty() {
            return new ResolvedProfileContext(null, null, null);
        }

        public boolean hasProfileContext() {
            return StringUtils.hasText(profileContext);
        }

        public boolean hasGrowthContext() {
            return growthContext != null && !growthContext.isEmpty();
        }
    }
}
