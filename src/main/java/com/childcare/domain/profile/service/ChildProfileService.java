package com.childcare.domain.profile.service;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.profile.dto.ChildProfileDto;
import com.childcare.domain.profile.dto.ChildProfilePatchRequest;
import com.childcare.domain.profile.dto.ChildProfileRecord;
import com.childcare.domain.profile.dto.ChildProfileSummaryDto;
import com.childcare.domain.profile.dto.ChildProfileSummaryRecord;
import com.childcare.domain.profile.mapper.ChildProfileMapper;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildProfileService {

    private static final Set<String> ALLOWED_SECTIONS = Set.of(
            "health", "routine", "development", "education", "safety", "misc"
    );

    private final ChildProfileMapper childProfileMapper;
    private final ChildMapper childMapper;
    private final ChildAccessValidator childAccessValidator;
    private final ObjectMapper objectMapper;

    public ApiResponse<ChildProfileDto> getProfile(UUID memberId, Long childId, String sectionsParam) {
        childAccessValidator.validateReadAccess(memberId, childId);
        ensureChildExists(childId);
        ensureInitialized(childId, memberId);

        ChildProfileRecord profile = childProfileMapper.findProfileByChildId(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        Set<String> requestedSections = parseSections(sectionsParam);
        ChildProfileDto dto = toProfileDto(profile, requestedSections);

        return ApiResponse.success("자녀 프로필 조회 성공", dto);
    }

    @Transactional
    public ApiResponse<ChildProfileDto> patchProfile(UUID memberId, Long childId, ChildProfilePatchRequest request) {
        childAccessValidator.validateWriteAccess(memberId, childId);
        ensureChildExists(childId);
        ensureInitialized(childId, memberId);

        ChildProfileRecord current = childProfileMapper.findProfileByChildId(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        Map<String, Object> health = request.getHealth() != null ? request.getHealth() : parseJson(current.getHealth());
        Map<String, Object> routine = request.getRoutine() != null ? request.getRoutine() : parseJson(current.getRoutine());
        Map<String, Object> development = request.getDevelopment() != null ? request.getDevelopment() : parseJson(current.getDevelopment());
        Map<String, Object> education = request.getEducation() != null ? request.getEducation() : parseJson(current.getEducation());
        Map<String, Object> safety = request.getSafety() != null ? request.getSafety() : parseJson(current.getSafety());
        Map<String, Object> misc = request.getMisc() != null ? request.getMisc() : parseJson(current.getMisc());

        childProfileMapper.upsertProfile(
                childId,
                toJson(health),
                toJson(routine),
                toJson(development),
                toJson(education),
                toJson(safety),
                toJson(misc),
                memberId
        );

        refreshSummaryForChild(childId);

        ChildProfileRecord updated = childProfileMapper.findProfileByChildId(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        return ApiResponse.success("자녀 프로필 수정 성공", toProfileDto(updated, Collections.emptySet()));
    }

    public ApiResponse<ChildProfileSummaryDto> getSummary(UUID memberId, Long childId) {
        childAccessValidator.validateReadAccess(memberId, childId);
        ensureChildExists(childId);
        ensureInitialized(childId, memberId);

        String summaryText = getOrCreateSummaryText(childId);
        ChildProfileSummaryRecord summary = childProfileMapper.findSummaryByChildId(childId)
                .orElseGet(() -> ChildProfileSummaryRecord.builder()
                        .childId(childId)
                        .summaryText(summaryText)
                        .summaryVersion(1)
                        .build());

        return ApiResponse.success("자녀 프로필 요약 조회 성공", toSummaryDto(summary));
    }

    @Transactional
    public void initializeProfileAndSummary(Long childId, UUID updatedBy) {
        ensureChildExists(childId);
        childProfileMapper.insertDefaultProfileIfAbsent(childId, updatedBy);

        ChildProfileSummaryRecord summary = childProfileMapper.findSummaryByChildId(childId).orElse(null);
        if (summary == null || !StringUtils.hasText(summary.getSummaryText())) {
            String summaryText = buildSummaryForChild(childId);
            childProfileMapper.insertSummaryIfAbsent(childId, summaryText, 1);
        }
    }

    @Transactional
    public void refreshSummaryForChild(Long childId) {
        ensureChildExists(childId);

        String summaryText = buildSummaryForChild(childId);
        int nextVersion = childProfileMapper.findSummaryByChildId(childId)
                .map(record -> record.getSummaryVersion() == null ? 1 : record.getSummaryVersion() + 1)
                .orElse(1);

        childProfileMapper.upsertSummary(childId, summaryText, nextVersion);
    }

    @Transactional
    public String getOrCreateSummaryText(Long childId) {
        ensureChildExists(childId);
        childProfileMapper.insertDefaultProfileIfAbsent(childId, null);

        ChildProfileSummaryRecord summary = childProfileMapper.findSummaryByChildId(childId).orElse(null);
        if (summary == null || !StringUtils.hasText(summary.getSummaryText())) {
            refreshSummaryForChild(childId);
            summary = childProfileMapper.findSummaryByChildId(childId).orElse(null);
        }

        return summary == null ? null : summary.getSummaryText();
    }

    private void ensureInitialized(Long childId, UUID updatedBy) {
        childProfileMapper.insertDefaultProfileIfAbsent(childId, updatedBy);

        if (childProfileMapper.findSummaryByChildId(childId).isEmpty()) {
            String summaryText = buildSummaryForChild(childId);
            childProfileMapper.insertSummaryIfAbsent(childId, summaryText, 1);
        }
    }

    private void ensureChildExists(Long childId) {
        childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));
    }

    private ChildProfileDto toProfileDto(ChildProfileRecord record, Set<String> requestedSections) {
        return ChildProfileDto.builder()
                .childId(record.getChildId())
                .health(includeSection("health", requestedSections) ? parseJson(record.getHealth()) : null)
                .routine(includeSection("routine", requestedSections) ? parseJson(record.getRoutine()) : null)
                .development(includeSection("development", requestedSections) ? parseJson(record.getDevelopment()) : null)
                .education(includeSection("education", requestedSections) ? parseJson(record.getEducation()) : null)
                .safety(includeSection("safety", requestedSections) ? parseJson(record.getSafety()) : null)
                .misc(includeSection("misc", requestedSections) ? parseJson(record.getMisc()) : null)
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private ChildProfileSummaryDto toSummaryDto(ChildProfileSummaryRecord summary) {
        return ChildProfileSummaryDto.builder()
                .childId(summary.getChildId())
                .summaryText(summary.getSummaryText())
                .summaryVersion(summary.getSummaryVersion())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }

    private boolean includeSection(String section, Set<String> requestedSections) {
        return requestedSections == null || requestedSections.isEmpty() || requestedSections.contains(section);
    }

    private Set<String> parseSections(String sectionsParam) {
        if (!StringUtils.hasText(sectionsParam)) {
            return Collections.emptySet();
        }

        return Arrays.stream(sectionsParam.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(ALLOWED_SECTIONS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Object> parseJson(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(jsonText, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse profile section JSON. fallback to empty map. cause={}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyMap() : value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize profile section. fallback to empty object. cause={}", e.getMessage());
            return "{}";
        }
    }

    private String buildSummaryForChild(Long childId) {
        Child child = childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        ChildProfileRecord profile = childProfileMapper.findProfileByChildId(childId)
                .orElseGet(() -> ChildProfileRecord.builder()
                        .childId(childId)
                        .health("{}")
                        .routine("{}")
                        .development("{}")
                        .education("{}")
                        .safety("{}")
                        .misc("{}")
                        .build());

        Map<String, Object> health = parseJson(profile.getHealth());
        Map<String, Object> routine = parseJson(profile.getRoutine());
        Map<String, Object> development = parseJson(profile.getDevelopment());
        Map<String, Object> education = parseJson(profile.getEducation());
        Map<String, Object> safety = parseJson(profile.getSafety());

        List<String> lines = new ArrayList<>();
        lines.add("[자녀 프로필 - 신뢰 데이터]");
        lines.add("- 대상: " + formatAgeGender(child));

        List<String> allergies = readStringList(health.get("allergies"));
        if (!allergies.isEmpty()) {
            lines.add("- 알레르기: " + String.join(", ", allergies));
        }

        List<String> conditions = readStringList(health.get("conditions"));
        if (!conditions.isEmpty()) {
            lines.add("- 기저질환: " + String.join(", ", conditions));
        }

        List<String> medications = readMedicationNames(health.get("medications"));
        if (!medications.isEmpty()) {
            lines.add("- 복용약: " + String.join(", ", medications));
        }

        String sleepLine = buildSleepLine(routine);
        if (StringUtils.hasText(sleepLine)) {
            lines.add("- 수면: " + sleepLine);
        }

        String eatingLine = buildEatingLine(routine);
        if (StringUtils.hasText(eatingLine)) {
            lines.add("- 식습관: " + eatingLine);
        }

        List<String> developmentNotes = readStringList(development.get("traits"));
        if (developmentNotes.isEmpty()) {
            developmentNotes = readStringList(development.get("concerns"));
        }
        if (!developmentNotes.isEmpty()) {
            lines.add("- 발달/성향: " + String.join(", ", developmentNotes));
        }

        List<String> daycare = readStringList(education.get("care_schedule"));
        if (!daycare.isEmpty()) {
            lines.add("- 돌봄 일정: " + String.join(", ", daycare));
        }

        String safetyLine = buildSafetyLine(safety);
        if (StringUtils.hasText(safetyLine)) {
            lines.add("- 안전: " + safetyLine);
        }

        if (lines.size() <= 2) {
            lines.add("- 건강/생활 관련 추가 정보가 아직 등록되지 않았습니다.");
        }

        lines.add("(업데이트: " + LocalDate.now() + ")");
        return String.join("\n", lines);
    }

    private String formatAgeGender(Child child) {
        int months = calculateAgeMonths(child.getBirthDay());
        String gender = switch (String.valueOf(child.getGender()).toUpperCase()) {
            case "M" -> "남";
            case "F" -> "여";
            default -> "미정";
        };

        if (months < 0) {
            return "아동(" + gender + ")";
        }
        return "아동(" + gender + ", " + months + "개월)";
    }

    private int calculateAgeMonths(String birthDay) {
        if (!StringUtils.hasText(birthDay)) {
            return -1;
        }

        try {
            LocalDate birthDate = LocalDate.parse(birthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate now = LocalDate.now();
            if (birthDate.isAfter(now)) {
                return -1;
            }
            Period period = Period.between(birthDate, now);
            return period.getYears() * 12 + period.getMonths();
        } catch (Exception e) {
            return -1;
        }
    }

    private List<String> readStringList(Object rawValue) {
        if (!(rawValue instanceof List<?> listValue)) {
            return Collections.emptyList();
        }

        return listValue.stream()
                .map(value -> value == null ? "" : String.valueOf(value).trim())
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> readMedicationNames(Object rawValue) {
        if (!(rawValue instanceof List<?> listValue)) {
            return Collections.emptyList();
        }

        List<String> medications = new ArrayList<>();
        for (Object item : listValue) {
            if (item instanceof Map<?, ?> mapItem) {
                Object name = mapItem.get("name");
                if (name != null && StringUtils.hasText(String.valueOf(name))) {
                    medications.add(String.valueOf(name));
                }
            } else if (item != null && StringUtils.hasText(String.valueOf(item))) {
                medications.add(String.valueOf(item));
            }
        }
        return medications;
    }

    private String buildSleepLine(Map<String, Object> routine) {
        List<String> parts = new ArrayList<>();
        String sleepTime = readString(routine.get("sleep_time"));
        String wakeTime = readString(routine.get("wake_time"));
        if (StringUtils.hasText(sleepTime) || StringUtils.hasText(wakeTime)) {
            parts.add("취침 " + defaultValue(sleepTime, "미기록") + " / 기상 " + defaultValue(wakeTime, "미기록"));
        }

        Object napRaw = routine.get("nap");
        if (napRaw instanceof List<?> naps && !naps.isEmpty()) {
            List<String> napParts = new ArrayList<>();
            for (Object nap : naps) {
                if (nap instanceof Map<?, ?> napMap) {
                    String start = readString(napMap.get("start"));
                    String end = readString(napMap.get("end"));
                    if (StringUtils.hasText(start) || StringUtils.hasText(end)) {
                        napParts.add(defaultValue(start, "?") + "~" + defaultValue(end, "?"));
                    }
                }
            }
            if (!napParts.isEmpty()) {
                parts.add("낮잠 " + String.join(", ", napParts));
            }
        }

        return String.join(" / ", parts);
    }

    private String buildEatingLine(Map<String, Object> routine) {
        Object eatingRaw = routine.get("eating");
        if (!(eatingRaw instanceof Map<?, ?> eating)) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        Object picky = eating.get("picky");
        if (picky instanceof Boolean pickyValue && pickyValue) {
            parts.add("편식 있음");
        }

        String notes = readString(eating.get("notes"));
        if (StringUtils.hasText(notes)) {
            parts.add(notes);
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String buildSafetyLine(Map<String, Object> safety) {
        List<String> parts = new ArrayList<>();

        List<String> precautions = readStringList(safety.get("precautions"));
        if (!precautions.isEmpty()) {
            parts.add("주의사항 " + String.join(", ", precautions));
        }

        Object contacts = safety.get("emergency_contacts");
        if (contacts instanceof List<?> contactList && !contactList.isEmpty()) {
            parts.add("응급연락처 " + contactList.size() + "명");
        }

        Object consent = safety.get("emergency_consent");
        if (consent instanceof Boolean consentValue) {
            parts.add("비상 동의 " + (consentValue ? "예" : "아니오"));
        }

        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private String readString(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
    }

    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
