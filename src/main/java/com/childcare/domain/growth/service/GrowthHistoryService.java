package com.childcare.domain.growth.service;

import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.growth.dto.GrowthHistoryRequest;
import com.childcare.domain.growth.dto.GrowthHistoryResponse;
import com.childcare.domain.growth.entity.ChildGrowHistory;
import com.childcare.domain.growth.mapper.GrowthHistoryMapper;
import com.childcare.global.exception.ChildException;
import com.childcare.global.exception.ChildException.ChildErrorCode;
import com.childcare.global.exception.GrowthHistoryException;
import com.childcare.global.exception.GrowthHistoryException.GrowthHistoryErrorCode;
import com.childcare.global.service.ChildAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GrowthHistoryService {

    private final GrowthHistoryMapper growthHistoryMapper;
    private final ChildMapper childMapper;
    private final ChildAccessValidator childAccessValidator;

    @Transactional
    public GrowthHistoryResponse createHistory(UUID memberId, Long childId, GrowthHistoryRequest request) {
        log.info("Creating growth history for child: {}", childId);
        validateRequest(request);
        validateChildWriteAccess(memberId, childId);

        ChildGrowHistory history = ChildGrowHistory.builder()
                .chSeq(childId)
                .height(request.getHeight())
                .weight(request.getWeight())
                .regId(memberId)
                .build();

        growthHistoryMapper.insertHistory(history);

        ChildGrowHistory savedHistory = growthHistoryMapper.findActiveHistoryById(childId, history.getGhSeq())
                .orElseThrow(() -> new GrowthHistoryException(GrowthHistoryErrorCode.NOT_FOUND));

        return toResponse(savedHistory);
    }

    @Transactional
    public GrowthHistoryResponse updateHistory(UUID memberId, Long childId, Long historyId, GrowthHistoryRequest request) {
        log.info("Updating growth history {} for child: {}", historyId, childId);
        validateRequest(request);
        validateChildWriteAccess(memberId, childId);

        ChildGrowHistory history = growthHistoryMapper.findActiveHistoryById(childId, historyId)
                .orElseThrow(() -> new GrowthHistoryException(GrowthHistoryErrorCode.NOT_FOUND));

        growthHistoryMapper.updateHistory(childId, historyId, request.getHeight(), request.getWeight());

        ChildGrowHistory updatedHistory = growthHistoryMapper.findActiveHistoryById(childId, history.getGhSeq())
                .orElseThrow(() -> new GrowthHistoryException(GrowthHistoryErrorCode.NOT_FOUND));

        return toResponse(updatedHistory);
    }

    public GrowthHistoryResponse getHistory(UUID memberId, Long childId, Long historyId) {
        log.info("Fetching growth history {} for child: {}", historyId, childId);
        validateChildReadAccess(memberId, childId);

        ChildGrowHistory history = growthHistoryMapper.findActiveHistoryById(childId, historyId)
                .orElseThrow(() -> new GrowthHistoryException(GrowthHistoryErrorCode.NOT_FOUND));

        return toResponse(history);
    }

    @Transactional
    public void deleteHistory(UUID memberId, Long childId, Long historyId) {
        log.info("Deleting growth history {} for child: {}", historyId, childId);
        validateChildWriteAccess(memberId, childId);

        ChildGrowHistory history = growthHistoryMapper.findActiveHistoryById(childId, historyId)
                .orElseThrow(() -> new GrowthHistoryException(GrowthHistoryErrorCode.NOT_FOUND));

        growthHistoryMapper.softDeleteHistory(childId, history.getGhSeq(), memberId);
    }

    private void validateChildWriteAccess(UUID memberId, Long childId) {
        childAccessValidator.validateWriteAccess(memberId, childId);

        childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));
    }

    private void validateChildReadAccess(UUID memberId, Long childId) {
        childAccessValidator.validateReadAccess(memberId, childId);

        childMapper.findActiveChildById(childId)
                .orElseThrow(() -> new ChildException(ChildErrorCode.NOT_FOUND));
    }

    private void validateRequest(GrowthHistoryRequest request) {
        if (request.getHeight() == null || request.getHeight().isBlank()) {
            throw new GrowthHistoryException(GrowthHistoryErrorCode.HEIGHT_REQUIRED);
        }
        if (request.getWeight() == null || request.getWeight().isBlank()) {
            throw new GrowthHistoryException(GrowthHistoryErrorCode.WEIGHT_REQUIRED);
        }
    }

    private GrowthHistoryResponse toResponse(ChildGrowHistory history) {
        return GrowthHistoryResponse.builder()
                .id(history.getGhSeq())
                .childId(history.getChSeq())
                .height(history.getHeight())
                .weight(history.getWeight())
                .recordedAt(history.getRegDate())
                .build();
    }
}
