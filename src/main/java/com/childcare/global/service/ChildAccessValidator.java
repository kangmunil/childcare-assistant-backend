package com.childcare.global.service;

import com.childcare.domain.parent.entity.Parent;
import com.childcare.domain.parent.repository.ParentRepository;
import com.childcare.global.exception.ChildAccessDeniedException;
import com.childcare.global.exception.ChildAccessDeniedException.AccessErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * 회원이 해당 자녀에 대한 조회 권한이 있는지 검증
     * @param memberSeq 회원 ID
     * @param childId 자녀 ID
     * @throws ChildAccessDeniedException 권한이 없는 경우
     */
    public void validateReadAccess(Long memberSeq, Long childId) {
        log.debug("Validating read access for member: {} to child: {}", memberSeq, childId);

        Optional<Parent> parentOpt = parentRepository.findByMbSeqAndChSeq(memberSeq, childId);

        if (parentOpt.isEmpty()) {
            log.warn("Access denied - member: {} has no access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS);
        }

        Parent parent = parentOpt.get();
        if (!"1".equals(parent.getAuthRead())) {
            log.warn("Read permission denied - member: {} has no read access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_READ_PERMISSION);
        }
    }

    /**
     * 회원이 해당 자녀에 대한 등록/수정 권한이 있는지 검증
     * @param memberSeq 회원 ID
     * @param childId 자녀 ID
     * @throws ChildAccessDeniedException 권한이 없는 경우
     */
    public void validateWriteAccess(Long memberSeq, Long childId) {
        log.debug("Validating write access for member: {} to child: {}", memberSeq, childId);

        Optional<Parent> parentOpt = parentRepository.findByMbSeqAndChSeq(memberSeq, childId);

        if (parentOpt.isEmpty()) {
            log.warn("Access denied - member: {} has no access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS);
        }

        Parent parent = parentOpt.get();
        if (!"1".equals(parent.getAuthWrite())) {
            log.warn("Write permission denied - member: {} has no write access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_WRITE_PERMISSION);
        }
    }

    /**
     * 회원이 해당 자녀에 대한 삭제 권한이 있는지 검증
     * @param memberSeq 회원 ID
     * @param childId 자녀 ID
     * @throws ChildAccessDeniedException 권한이 없는 경우
     */
    public void validateDeleteAccess(Long memberSeq, Long childId) {
        log.debug("Validating delete access for member: {} to child: {}", memberSeq, childId);

        Optional<Parent> parentOpt = parentRepository.findByMbSeqAndChSeq(memberSeq, childId);

        if (parentOpt.isEmpty()) {
            log.warn("Access denied - member: {} has no access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS);
        }

        Parent parent = parentOpt.get();
        if (!"1".equals(parent.getAuthDelete())) {
            log.warn("Delete permission denied - member: {} has no delete access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_DELETE_PERMISSION);
        }
    }

    /**
     * 회원이 해당 자녀에 대한 관리 권한이 있는지 검증
     * @param memberSeq 회원 ID
     * @param childId 자녀 ID
     * @throws ChildAccessDeniedException 권한이 없는 경우
     */
    public void validateManageAccess(Long memberSeq, Long childId) {
        log.debug("Validating manage access for member: {} to child: {}", memberSeq, childId);

        Optional<Parent> parentOpt = parentRepository.findByMbSeqAndChSeq(memberSeq, childId);

        if (parentOpt.isEmpty()) {
            log.warn("Access denied - member: {} has no access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_ACCESS);
        }

        Parent parent = parentOpt.get();
        if (!"1".equals(parent.getAuthManage())) {
            log.warn("Manage permission denied - member: {} has no manage access to child: {}", memberSeq, childId);
            throw new ChildAccessDeniedException(AccessErrorCode.NO_MANAGE_PERMISSION);
        }
    }
}
