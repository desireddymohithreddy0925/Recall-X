package com.recallx.recallx.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordIdsTest {

    @Test
    void extractsEachIdOnceInOrder() {
        assertThat(RecordIds.extract("See INC-18, ADR-7 and DEP-2026-017; INC-18 again. WARN-42."))
                .containsExactly("INC-18", "ADR-7", "DEP-2026-017", "WARN-42");
    }

    @Test
    void recognisesADocumentIdThatIsExactlyOneRecordId() {
        assertThat(RecordIds.isRecordId("INC-18")).isTrue();
        assertThat(RecordIds.isRecordId("DEP-2026-017")).isTrue();
        assertThat(RecordIds.isRecordId("doc-INC-18")).isFalse();
        assertThat(RecordIds.isRecordId("SPIKE-TEMP")).isFalse();
        assertThat(RecordIds.isRecordId(null)).isFalse();
    }

    @Test
    void deploymentsAloneAreNotEvidence() {
        assertThat(RecordIds.isEvidence("INC-18")).isTrue();
        assertThat(RecordIds.isEvidence("ADR-7")).isTrue();
        assertThat(RecordIds.isEvidence("WARN-42")).isTrue();
        assertThat(RecordIds.isEvidence("DEP-2026-017")).isFalse();
    }
}
