package com.childcare.domain.profile.service;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.profile.dto.ChildProfileRecord;
import com.childcare.domain.profile.mapper.ChildProfileMapper;
import com.childcare.global.service.ChildAccessValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChildProfileServiceTest {

    private final ChildProfileMapper childProfileMapper = mock(ChildProfileMapper.class);
    private final ChildMapper childMapper = mock(ChildMapper.class);
    private final ChildAccessValidator childAccessValidator = mock(ChildAccessValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChildProfileService service = new ChildProfileService(
            childProfileMapper,
            childMapper,
            childAccessValidator,
            objectMapper
    );

    @Test
    void refreshSummaryForChildBuildsTemplateAndMasksPii() {
        Long childId = 7L;

        when(childMapper.findActiveChildById(childId)).thenReturn(Optional.of(
                Child.builder()
                        .chSeq(childId)
                        .name("테스트아동")
                        .birthDay("2024-01-15")
                        .gender("M")
                        .build()
        ));

        when(childProfileMapper.findProfileByChildId(childId)).thenReturn(Optional.of(
                ChildProfileRecord.builder()
                        .childId(childId)
                        .health("{\"allergies\":[\"계란\"],\"conditions\":[\"아토피\"]}")
                        .routine("{\"sleep_time\":\"21:30\",\"wake_time\":\"06:30\",\"eating\":{\"picky\":true,\"notes\":\"채소 거부\"}}")
                        .development("{}")
                        .education("{}")
                        .safety("{}")
                        .misc("{}")
                        .build()
        ));

        when(childProfileMapper.findSummaryByChildId(childId)).thenReturn(Optional.empty());

        service.refreshSummaryForChild(childId);

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(childProfileMapper).upsertSummary(eq(childId), summaryCaptor.capture(), anyInt());
        String generated = summaryCaptor.getValue();

        assertTrue(generated.contains("알레르기: 계란"));
        assertTrue(generated.contains("식습관: 편식 있음, 채소 거부"));
        assertTrue(generated.contains("[자녀 프로필 - 신뢰 데이터]"));
        assertFalse(generated.contains("테스트아동"));
        assertFalse(generated.contains("2024-01-15"));
    }
}
