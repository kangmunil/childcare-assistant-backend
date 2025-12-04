package com.childcare.global.service;

import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.exception.ChildAccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChildAccessValidator {

    private final ParentRepository parentRepository;

    /**
     * 회원이 해당 자녀에 대한 접근 권한이 있는지 검증
     * @param memberSeq 회원 ID
     * @param childId 자녀 ID
     * @throws ChildAccessDeniedException 권한이 없는 경우
     */
    public void validateAccess(Long memberSeq, Long childId) {
        log.debug("Validating access for member: {} to child: {}", memberSeq, childId);

        boolean hasAccess = parentRepository.findByMbSeqAndChSeq(memberSeq, childId).isPresent();

        if (!hasAccess) {
            log.warn("Access denied - member: {} has no access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException();
        }
    }
}
