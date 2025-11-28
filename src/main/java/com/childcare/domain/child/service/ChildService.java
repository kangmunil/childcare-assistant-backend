package com.childcare.domain.child.service;

import com.childcare.domain.child.dto.ChildResponse;
import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;

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

    private ChildResponse.ChildDto toDto(Child child) {
        // gender 변환: M -> male, F -> female
        String genderStr = "M".equals(child.getGender()) ? "male" : "female";

        // 프로필 이미지 URL 생성 (이름 기반 아바타)
        String photoUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + child.getName();

        return ChildResponse.ChildDto.builder()
                .id(child.getChSeq())
                .name(child.getName())
                .birthDate(child.getBirthDay())
                .gender(genderStr)
                .photoUrl(photoUrl)
                .build();
    }
}
