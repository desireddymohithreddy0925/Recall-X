package com.recallx.recallx.memory;

import com.recallx.recallx.store.Warning;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryTemplatesTest {

    private static Warning falsePositive(String reason) {
        return new Warning("WARN-42", "DEP-2026-041", "payment.retry.max-attempts", Instant.parse("2026-06-16T11:00:00Z"),
                "DECISION_GUARD", "SEV1", "Resembles INC-29.", List.of("ADR-9", "INC-29"), List.of(), "", "",
                "FALSE_POSITIVE", reason, Instant.parse("2026-06-16T13:00:00Z"));
    }

    @Test
    void verdictEndsWithExactlyOneFullStop() {
        assertThat(MemoryTemplates.warningOutcome(falsePositive("Idempotency is on.")))
                .endsWith("The warning did not apply: Idempotency is on.")
                .doesNotContain("..");
        assertThat(MemoryTemplates.warningOutcome(falsePositive("Idempotency is on")))
                .endsWith("The warning did not apply: Idempotency is on.");
    }

    @Test
    void outcomeCarriesItsOwnIdAndDate() {
        assertThat(MemoryTemplates.warningOutcome(falsePositive("x")))
                .startsWith("[WARNING OUTCOME WARN-42] deployment=DEP-2026-041 date=2026-06-16 key=payment.retry.max-attempts");
    }
}
