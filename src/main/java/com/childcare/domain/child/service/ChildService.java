package com.childcare.domain.child.service;

import com.childcare.domain.child.dto.ChildRequest;
import com.childcare.domain.child.dto.ChildResponse;
import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.repository.ChildRepository;
import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
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

    public ChildResponse getChildrenByMemberSeq(Long memberSeq) {
        log.info("Fetching children for member: {}", memberSeq);

        List<Child> children = childRepository.findActiveChildrenByMemberSeq(memberSeq);

        List<ChildResponse.ChildDto> childDtos = children.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ChildResponse.builder()
                .status("success")
                .message("자녀 목록 조회 성공")
                .data(childDtos)
                .build();
    }

    @Transactional
    public ChildResponse createChild(Long memberSeq, ChildRequest request) {
        log.info("Creating child for member: {}", memberSeq);

        // 필수값 검증
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("이름은 필수 입력값입니다.");
        }
        if (request.getBirthDay() == null || request.getBirthDay().isBlank()) {
            throw new IllegalArgumentException("생년월일은 필수 입력값입니다.");
        }
        if (request.getBirthTime() == null || request.getBirthTime().isBlank()) {
            throw new IllegalArgumentException("태어난 시각은 필수 입력값입니다.");
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
                .relation("PARENT")
                .build();

        parentRepository.save(parent);

        return ChildResponse.builder()
                .status("success")
                .message("자녀 등록 성공")
                .data(List.of(toDto(savedChild)))
                .build();
    }

    @Transactional
    public ChildResponse updateChild(Long memberSeq, Long childId, ChildRequest request) {
        log.info("Updating child {} for member: {}", childId, memberSeq);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀 정보를 찾을 수 없습니다."));

        if ("Y".equals(child.getDeleteYn())) {
            throw new IllegalArgumentException("삭제된 자녀 정보입니다.");
        }

        child.setName(request.getName());
        child.setBirthDay(request.getBirthDay());
        child.setBirthTime(request.getBirthTime());
        child.setGender(request.getGender());
        child.setHeight(request.getHeight());
        child.setWeight(request.getWeight());
        child.setMemo(request.getMemo());

        Child updatedChild = childRepository.save(child);

        return ChildResponse.builder()
                .status("success")
                .message("자녀 정보 수정 성공")
                .data(List.of(toDto(updatedChild)))
                .build();
    }

    @Transactional
    public ChildResponse deleteChild(Long memberSeq, Long childId) {
        log.info("Deleting child {} for member: {}", childId, memberSeq);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀 정보를 찾을 수 없습니다."));

        if ("Y".equals(child.getDeleteYn())) {
            throw new IllegalArgumentException("이미 삭제된 자녀 정보입니다.");
        }

        child.setDeleteYn("Y");
        child.setDeleteUserSeq(String.valueOf(memberSeq));
        child.setDeleteDate(LocalDateTime.now());

        childRepository.save(child);

        return ChildResponse.builder()
                .status("success")
                .message("자녀 정보 삭제 성공")
                .data(null)
                .build();
    }

    private ChildResponse.ChildDto toDto(Child child) {
        // gender 변환: M -> male, F -> female
        String genderStr = "M".equals(child.getGender()) ? "male" : "female";

        // 프로필 이미지 URL 생성 (이름 기반 아바타)
        String photoUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + child.getName();

        return ChildResponse.ChildDto.builder()
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
