package com.childcare.domain.child.service;

import com.childcare.domain.child.dto.ChildDto;
import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.GrowthHistoryDto;
import com.childcare.domain.child.dto.GrowthHistoryStatDto;
import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.entity.ChildImage;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.child.repository.ChildImageRepository;
import com.childcare.domain.child.repository.ChildRepository;
import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import com.childcare.global.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ChildImageRepository childImageRepository;
    private final ParentRepository parentRepository;
    private final ChildMapper childMapper;
    private final SupabaseStorageService storageService;

    @Value("${supabase.storage.child-image-bucket}")
    private String childImageBucket;

    public ApiResponse<List<ChildDto>> getChildrenByMemberId(UUID memberId) {
        log.info("Fetching children for member: {}", memberId);

        List<Child> children = childMapper.findActiveChildrenByMemberId(memberId);

        List<ChildDto> childDtos = children.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("자녀 목록 조회 성공", childDtos);
    }

    public ApiResponse<ChildDto> getChildById(UUID memberId, Long childId) {
        log.info("Fetching child {} for member: {}", childId, memberId);

        // 해당 회원의 자녀인지 확인
        List<Child> children = childMapper.findActiveChildrenByMemberId(memberId);
        boolean hasAccess = children.stream()
                .anyMatch(c -> c.getChSeq().equals(childId));

        if (!hasAccess) {
            throw new ChildException(ChildErrorCode.NOT_FOUND);
        }

        Child child = childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        return ApiResponse.success("자녀 정보 조회 성공", toDto(child));
    }

    @Transactional
    public ApiResponse<List<ChildDto>> createChild(UUID memberId, ChildRequest request) {
        log.info("Creating child for member: {}", memberId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ChildException(ChildErrorCode.NAME_REQUIRED);
        }
        if (request.getBirthDay() == null || request.getBirthDay().isBlank()) {
            throw new ChildException(ChildErrorCode.BIRTHDAY_REQUIRED);
        }
        if (request.getBirthTime() == null || request.getBirthTime().isBlank()) {
            throw new ChildException(ChildErrorCode.BIRTHTIME_REQUIRED);
        }

        Child child = Child.builder()
                .name(request.getName())
                .birthDay(request.getBirthDay())
                .birthTime(request.getBirthTime())
                .gender(request.getGender())
                .height(request.getHeight())
                .weight(request.getWeight())
                .memo(request.getMemo())
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .deleteYn("N")
                .build();

        Child savedChild = childRepository.save(child);

        Parent parent = Parent.builder()
                .mbId(memberId)
                .chSeq(savedChild.getChSeq())
                .relation("family")
                .authManage("1")
                .authRead("1")
                .authWrite("1")
                .authDelete("1")
                .regId(memberId)
                .regDate(LocalDateTime.now())
                .build();

        parentRepository.save(parent);

        return ApiResponse.success("자녀 등록 성공", List.of(toDto(savedChild)));
    }

    @Transactional
    public ApiResponse<List<ChildDto>> updateChild(UUID memberId, Long childId, ChildRequest request) {
        log.info("Updating child {} for member: {}", childId, memberId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if ("Y".equals(child.getDeleteYn())) {
            throw new ChildException(ChildErrorCode.ALREADY_DELETED);
        }

        child.setName(request.getName());
        child.setBirthDay(request.getBirthDay());
        child.setBirthTime(request.getBirthTime());
        // gender: "male"/"female" → "M"/"F" 변환
        String gender = request.getGender();
        if ("male".equalsIgnoreCase(gender)) gender = "M";
        else if ("female".equalsIgnoreCase(gender)) gender = "F";
        child.setGender(gender);
        child.setHeight(request.getHeight());
        child.setWeight(request.getWeight());
        child.setMemo(request.getMemo());

        Child updatedChild = childRepository.save(child);

        return ApiResponse.success("자녀 정보 수정 성공", List.of(toDto(updatedChild)));
    }

    @Transactional
    public ApiResponse<Void> deleteChild(UUID memberId, Long childId) {
        log.info("Deleting child {} for member: {}", childId, memberId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if ("Y".equals(child.getDeleteYn())) {
            throw new ChildException(ChildErrorCode.ALREADY_DELETED);
        }

        // auth_manage 권한 확인
        Parent myRelation = parentRepository.findByMbIdAndChSeq(memberId, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NO_DELETE_PERMISSION));

        if (!"1".equals(myRelation.getAuthManage())) {
            throw new ChildException(ChildErrorCode.NO_DELETE_PERMISSION);
        }

        // 모든 가족 관계 삭제
        var parentRelations = parentRepository.findByChSeq(childId);
        parentRepository.deleteAll(parentRelations);

        // 자녀 삭제
        child.setDeleteYn("Y");
        child.setDeleteId(memberId);
        child.setDeleteDate(LocalDateTime.now());

        childRepository.save(child);

        return ApiResponse.success("자녀 정보 삭제 성공", null);
    }

    /**
     * 가족 관계 삭제
     */
    @Transactional
    public ApiResponse<Void> deleteParentRelation(UUID memberId, Long childId, UUID targetMbId) {
        log.info("Deleting parent relation for child {} member {} by {}", childId, targetMbId, memberId);

        // 요청자의 auth_manage 권한 확인
        Parent myRelation = parentRepository.findByMbIdAndChSeq(memberId, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if (!"1".equals(myRelation.getAuthManage())) {
            throw new ChildException(ChildErrorCode.NO_PARENT_DELETE_PERMISSION);
        }

        // 삭제할 가족 관계 조회
        Parent targetRelation = parentRepository.findByMbIdAndChSeq(targetMbId, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.PARENT_NOT_FOUND));

        parentRepository.delete(targetRelation);

        return ApiResponse.success("가족 관계 삭제 성공", null);
    }

    public ApiResponse<List<GrowthHistoryDto>> getGrowthHistory(UUID memberId, Long childId) {
        log.info("Fetching growth history for child: {}", childId);

        // 해당 회원의 자녀인지 확인
        List<Child> children = childMapper.findActiveChildrenByMemberId(memberId);
        boolean hasAccess = children.stream()
                .anyMatch(c -> c.getChSeq().equals(childId));

        if (!hasAccess) {
            throw new ChildException(ChildErrorCode.NOT_FOUND);
        }

        List<GrowthHistoryDto> history = childMapper.findGrowthHistory(childId);

        return ApiResponse.success("성장 이력 조회 성공", history);
    }

    /**
     * 기간별 성장 통계 조회
     * @param memberId 회원 ID
     * @param childId 자녀 ID
     * @param periodType 기간 타입 (week, month, year)
     * @param startDate 시작일 (YYYY-MM-DD)
     * @param endDate 종료일 (YYYY-MM-DD)
     */
    public ApiResponse<GrowthHistoryStatDto> getGrowthHistoryStats(UUID memberId, Long childId, String periodType, String startDate, String endDate) {
        log.info("Fetching growth history stats for child: {} periodType: {} from {} to {}", childId, periodType, startDate, endDate);

        // 해당 회원의 자녀인지 확인
        List<Child> children = childMapper.findActiveChildrenByMemberId(memberId);
        boolean hasAccess = children.stream()
                .anyMatch(c -> c.getChSeq().equals(childId));

        if (!hasAccess) {
            throw new ChildException(ChildErrorCode.NOT_FOUND);
        }

        List<GrowthHistoryStatDto.GrowthStat> stats = childMapper.findGrowthHistoryStatsByPeriod(childId, periodType, startDate, endDate);

        GrowthHistoryStatDto data = GrowthHistoryStatDto.builder()
                .childId(childId)
                .periodType(periodType)
                .startDate(startDate)
                .endDate(endDate)
                .stats(stats)
                .build();

        return ApiResponse.success("성장 통계 조회 성공", data);
    }

    private ChildDto toDto(Child child) {
        String genderStr = "M".equals(child.getGender()) ? "male" : "female";

        // 업로드된 프로필 이미지가 있으면 서명된 URL, 없으면 빈 문자열
        String photoUrl = "";
        List<ChildImage> images = childImageRepository.findAllByChSeq(child.getChSeq());
        if (!images.isEmpty()) {
            try {
                photoUrl = storageService.getSignedUrl(images.get(0).getCiPath(), 3600, childImageBucket);
            } catch (Exception e) {
                log.warn("Failed to get signed URL for child image (childId={}): {}", child.getChSeq(), e.getMessage());
            }
        }

        return ChildDto.builder()
                .id(child.getChSeq())
                .name(child.getName())
                .birthDate(child.getBirthDay())
                .birthTime(child.getBirthTime())
                .gender(genderStr)
                .height(child.getHeight())
                .weight(child.getWeight())
                .photoUrl(photoUrl)
                .build();
    }
}
