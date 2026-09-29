package com.recallx.recallx.service;

import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.RecallHit;
import com.recallx.recallx.memory.hindsight.RecallRequest;
import com.recallx.recallx.memory.hindsight.RecallResponse;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatternServiceTest {

    private final HindsightClient memory = mock(HindsightClient.class);
    private final RecordLookup lookup = mock(RecordLookup.class);
    private final PatternService service = new PatternService(memory, lookup);

    @Test
    void anObservationIsShownWithTheRealRecordsItWasBuiltFrom() {
        RecallHit observation = new RecallHit("o1", "Pool-size changes keep causing connection errors.", "observation",
                null, null, 0.9, 0.9, List.of("f1", "f2", "f3"));
        when(memory.recall(any(RecallRequest.class))).thenReturn(new RecallResponse(List.of(observation), Map.of(
                "f1", RecallHit.of("INC-18", "pool raised to 50", 0.9),
                "f2", RecallHit.of("INC-31", "connection timeout lowered", 0.8),
                "f3", RecallHit.of("INC-77", "an ID the database doesn't have", 0.7))));
        when(lookup.describe(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter(id -> !id.equals("INC-77"))
                    .map(id -> new RecordSummary(id, "INCIDENT", "2026-03-12", "SEV2", "t")).toList();
        });

        PatternService.PatternsResponse response = service.patterns("payment-service");

        assertThat(response.memoryAvailable()).isTrue();
        assertThat(response.patterns()).hasSize(1);
        assertThat(response.patterns().get(0).records()).extracting(RecordSummary::id).containsExactly("INC-18", "INC-31");
    }

    @Test
    void memoryDownIsReportedNotHidden() {
        when(memory.recall(any(RecallRequest.class))).thenThrow(new MemoryUnavailableException("down", 503, null));

        PatternService.PatternsResponse response = service.patterns("payment-service");

        assertThat(response.memoryAvailable()).isFalse();
        assertThat(response.patterns()).isEmpty();
    }
}
