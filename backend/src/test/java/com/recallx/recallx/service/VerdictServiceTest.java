package com.recallx.recallx.service;

import com.recallx.recallx.common.BadRequestException;
import com.recallx.recallx.common.ConflictException;
import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.store.Warning;
import com.recallx.recallx.store.WarningStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerdictServiceTest {

    private final WarningStore warnings = mock(WarningStore.class);
    private final MemorySyncService memorySync = mock(MemorySyncService.class);
    private final VerdictService verdicts = new VerdictService(warnings, memorySync);

    private final Warning warn55 = new Warning("WARN-55", "DEP-2026-061", "spring.datasource.hikari.maximum-pool-size",
            Instant.now(), "DECISION_GUARD", "SEV2", "Set deliberately in ADR-7.", List.of("ADR-7", "INC-18"),
            List.of(), "", "", "PENDING", null, null);

    @Test
    void aFalsePositiveNeedsAReason() {
        assertThatThrownBy(() -> verdicts.record("WARN-55", "FALSE_POSITIVE", " "))
                .isInstanceOf(BadRequestException.class);
        verify(warnings, never()).recordVerdict(anyString(), anyString(), any(), any());
    }

    @Test
    void aSecondVerdictIsRefused() {
        when(warnings.findById("WARN-55")).thenReturn(Optional.of(warn55));
        when(warnings.recordVerdict(eq("WARN-55"), eq("USEFUL"), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> verdicts.record("WARN-55", "USEFUL", null)).isInstanceOf(ConflictException.class);
        verify(memorySync, never()).retainWarningOutcome(anyString());
    }

    @Test
    void everyVerdictIsRetainedUnderTheWarningId() {
        when(warnings.findById("WARN-55")).thenReturn(Optional.of(warn55));
        when(warnings.recordVerdict(eq("WARN-55"), eq("USEFUL"), any(), any())).thenReturn(1);
        when(memorySync.retainWarningOutcome("WARN-55")).thenReturn(true);

        VerdictService.VerdictResponse response = verdicts.record("WARN-55", "USEFUL", null);

        assertThat(response.retained()).isTrue();
        verify(memorySync).retainWarningOutcome("WARN-55");
    }
}
