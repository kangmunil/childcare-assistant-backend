package com.childcare.domain.profile.service;

import com.childcare.domain.child.entity.Child;
import com.childcare.domain.child.mapper.ChildMapper;
import com.childcare.domain.growth.entity.ChildGrowHistory;
import com.childcare.domain.growth.mapper.GrowthHistoryMapper;
import com.childcare.global.exception.ChildAccessDeniedException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChildProfilePromptServiceTest {

    private final ChildMapper childMapper = mock(ChildMapper.class);
    private final GrowthHistoryMapper growthHistoryMapper = mock(GrowthHistoryMapper.class);
    private final ChildProfileService childProfileService = mock(ChildProfileService.class);
    private final ChildProfilePromptService promptService = new ChildProfilePromptService(childMapper, growthHistoryMapper, childProfileService);

    @Test
    void resolveProfileContextUsesRequestedChildWhenAccessible() {
        UUID memberId = UUID.randomUUID();
        Long childId = 10L;

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of(
                Child.builder()
                        .chSeq(childId)
                        .gender("M")
                        .birthDay("2024-01-01")
                        .height("82.5")
                        .weight("11.3")
                        .build()
        ));
        when(growthHistoryMapper.findLatestActiveHistoryByChildId(childId)).thenReturn(java.util.Optional.empty());
        when(childProfileService.getOrCreateSummaryText(childId)).thenReturn("summary");

        ChildProfilePromptService.ResolvedProfileContext context = promptService.resolveProfileContext(memberId, childId);

        assertEquals(childId, context.getChildId());
        assertEquals("summary", context.getProfileContext());
        assertEquals("M", context.getGrowthContext().get("gender"));
        assertEquals("2024-01-01", context.getGrowthContext().get("birth_date"));
        assertEquals(82.5d, (Double) context.getGrowthContext().get("height_cm"));
        assertEquals(11.3d, (Double) context.getGrowthContext().get("weight_kg"));
        assertEquals("child_profile", context.getGrowthContext().get("data_source"));
        verify(childProfileService).getOrCreateSummaryText(childId);
    }

    @Test
    void resolveProfileContextThrowsWhenRequestedChildIsNotAccessible() {
        UUID memberId = UUID.randomUUID();

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of(
                Child.builder().chSeq(1L).build()
        ));
        when(growthHistoryMapper.findLatestActiveHistoryByChildId(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(
                ChildAccessDeniedException.class,
                () -> promptService.resolveProfileContext(memberId, 2L)
        );
    }

    @Test
    void resolveProfileContextFallsBackToFirstReadableChild() {
        UUID memberId = UUID.randomUUID();

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of(
                Child.builder().chSeq(3L).gender("F").birthDay("2023-01-01").build(),
                Child.builder().chSeq(4L).gender("M").birthDay("2022-01-01").build()
        ));
        when(growthHistoryMapper.findLatestActiveHistoryByChildId(3L)).thenReturn(java.util.Optional.empty());
        when(childProfileService.getOrCreateSummaryText(3L)).thenReturn("fallback-summary");

        ChildProfilePromptService.ResolvedProfileContext context = promptService.resolveProfileContext(memberId, null);

        assertEquals(3L, context.getChildId());
        assertEquals("fallback-summary", context.getProfileContext());
    }

    @Test
    void resolveProfileContextReturnsEmptyWhenNoReadableChildren() {
        UUID memberId = UUID.randomUUID();

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of());

        ChildProfilePromptService.ResolvedProfileContext context = promptService.resolveProfileContext(memberId, null);

        assertNull(context.getChildId());
        assertNull(context.getProfileContext());
    }

    @Test
    void resolveProfileContextThrowsWhenRequestedChildExistsButNoReadableChildren() {
        UUID memberId = UUID.randomUUID();

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of());

        assertThrows(
                ChildAccessDeniedException.class,
                () -> promptService.resolveProfileContext(memberId, 99L)
        );
    }

    @Test
    void resolveProfileContextUsesLatestGrowthHistoryForMeasurements() {
        UUID memberId = UUID.randomUUID();
        Long childId = 77L;

        when(childMapper.findActiveChildrenByMemberId(memberId)).thenReturn(List.of(
                Child.builder()
                        .chSeq(childId)
                        .gender("F")
                        .birthDay("2023-08-01")
                        .height("0")
                        .weight("0")
                        .build()
        ));
        when(childProfileService.getOrCreateSummaryText(childId)).thenReturn("summary");
        when(growthHistoryMapper.findLatestActiveHistoryByChildId(childId)).thenReturn(java.util.Optional.of(
                ChildGrowHistory.builder()
                        .chSeq(childId)
                        .height("90.2")
                        .weight("13.7")
                        .ghDate("2026-02-01")
                        .regDate(LocalDateTime.parse("2026-02-01T09:00:00"))
                        .build()
        ));

        ChildProfilePromptService.ResolvedProfileContext context = promptService.resolveProfileContext(memberId, childId);

        assertEquals("history", context.getGrowthContext().get("data_source"));
        assertEquals(90.2d, (Double) context.getGrowthContext().get("height_cm"));
        assertEquals(13.7d, (Double) context.getGrowthContext().get("weight_kg"));
        assertEquals("2026-02-01", context.getGrowthContext().get("measured_date"));
        assertTrue((Integer) context.getGrowthContext().get("stale_days") >= 0);
    }
}
