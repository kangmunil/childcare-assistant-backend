package com.childcare.domain.child.service;

import com.childcare.domain.child.dto.ChildDto;
import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.GrowthHistoryDto;
import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.child.repository.ChildRepository;
import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.dto.ApiResponse;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final ChildMapper childMapper;

    public ApiResponse<List<ChildDto>> getChildrenByMemberSeq(Long memberSeq) {
        log.info("Fetching children for member: {}", memberSeq);

        List<Child> children = childMapper.findActiveChildrenByMemberSeq(memberSeq);

        List<ChildDto> childDtos = children.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("자녀 목록 조회 성공", childDtos);
    }

    public ApiResponse<ChildDto> getChildById(Long memberSeq, Long childId) {
        log.info("Fetching child {} for member: {}", childId, memberSeq);

        // 해당 회원의 자녀인지 확인
        List<Child> children = childMapper.findActiveChildrenByMemberSeq(memberSeq);
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
    public ApiResponse<List<ChildDto>> createChild(Long memberSeq, ChildRequest request) {
        log.info("Creating child for member: {}", memberSeq);

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
                .regUserSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .deleteYn("N")
                .build();

        Child savedChild = childRepository.save(child);

        Parent parent = Parent.builder()
                .mbSeq(memberSeq)
                .chSeq(savedChild.getChSeq())
                .relation("family")
                .authManage("1")
                .authRead("1")
                .authWrite("1")
                .authDelete("1")
                .regUserSeq(memberSeq)
                .regDate(LocalDateTime.now())
                .build();

        parentRepository.save(parent);

        return ApiResponse.success("자녀 등록 성공", List.of(toDto(savedChild)));
    }

    @Transactional
    public ApiResponse<List<ChildDto>> updateChild(Long memberSeq, Long childId, ChildRequest request) {
        log.info("Updating child {} for member: {}", childId, memberSeq);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if ("Y".equals(child.getDeleteYn())) {
            throw new ChildException(ChildErrorCode.ALREADY_DELETED);
        }

        child.setName(request.getName());
        child.setBirthDay(request.getBirthDay());
        child.setBirthTime(request.getBirthTime());
        child.setGender(request.getGender());
        child.setHeight(request.getHeight());
        child.setWeight(request.getWeight());
        child.setMemo(request.getMemo());

        Child updatedChild = childRepository.save(child);

        return ApiResponse.success("자녀 정보 수정 성공", List.of(toDto(updatedChild)));
    }

    @Transactional
    public ApiResponse<Void> deleteChild(Long memberSeq, Long childId) {
        log.info("Deleting child {} for member: {}", childId, memberSeq);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if ("Y".equals(child.getDeleteYn())) {
            throw new ChildException(ChildErrorCode.ALREADY_DELETED);
        }
/*
        // 1. 등록자(reg_user_seq)인지 확인
        if (!child.getRegUserSeq().equals(memberSeq)) {
            throw new ChildException(ChildErrorCode.NO_DELETE_PERMISSION);
        }
*/
        // 2. auth_manage 권한 확인
        Parent myRelation = parentRepository.findByMbSeqAndChSeq(memberSeq, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NO_DELETE_PERMISSION));

        if (!"1".equals(myRelation.getAuthManage())) {
            throw new ChildException(ChildErrorCode.NO_DELETE_PERMISSION);
        }

        // 3. 모든 가족 관계 삭제
        var parentRelations = parentRepository.findByChSeq(childId);
        parentRepository.deleteAll(parentRelations);

        // 4. 자녀 삭제
        child.setDeleteYn("Y");
        child.setDeleteUserSeq(String.valueOf(memberSeq));
        child.setDeleteDate(LocalDateTime.now());

        childRepository.save(child);

        return ApiResponse.success("자녀 정보 삭제 성공", null);
    }

    /**
     * 가족 관계 삭제
     */
    @Transactional
    public ApiResponse<Void> deleteParentRelation(Long memberSeq, Long childId, Long targetMbSeq) {
        log.info("Deleting parent relation for child {} member {} by {}", childId, targetMbSeq, memberSeq);

        // 요청자의 auth_manage 권한 확인
        Parent myRelation = parentRepository.findByMbSeqAndChSeq(memberSeq, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));

        if (!"1".equals(myRelation.getAuthManage())) {
            throw new ChildException(ChildErrorCode.NO_PARENT_DELETE_PERMISSION);
        }

        // 삭제할 가족 관계 조회
        Parent targetRelation = parentRepository.findByMbSeqAndChSeq(targetMbSeq, childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.PARENT_NOT_FOUND));

        parentRepository.delete(targetRelation);

        return ApiResponse.success("가족 관계 삭제 성공", null);
    }

    public ApiResponse<List<GrowthHistoryDto>> getGrowthHistory(Long memberSeq, Long childId) {
        log.info("Fetching growth history for child: {}", childId);

        // 해당 회원의 자녀인지 확인
        List<Child> children = childMapper.findActiveChildrenByMemberSeq(memberSeq);
        boolean hasAccess = children.stream()
                .anyMatch(c -> c.getChSeq().equals(childId));

        if (!hasAccess) {
            throw new ChildException(ChildErrorCode.NOT_FOUND);
        }

        List<GrowthHistoryDto> history = childMapper.findGrowthHistory(childId);

        return ApiResponse.success("성장 이력 조회 성공", history);
    }

    private ChildDto toDto(Child child) {
        String genderStr = "M".equals(child.getGender()) ? "male" : "female";
        String photoUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + child.getName();

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
