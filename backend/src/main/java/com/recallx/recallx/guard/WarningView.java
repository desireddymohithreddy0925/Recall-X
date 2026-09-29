package com.recallx.recallx.guard;

import com.recallx.recallx.memory.hindsight.Sources;
import com.recallx.recallx.store.RecordSummary;
import com.recallx.recallx.store.Warning;

import java.util.List;

/**
 * A warning as the UI and the MCP tool see it. id is null for a dry run.
 *
 * @param foundBy       DECISION (an active decision governs the key) or MEMORY (found by Hindsight recall alone)
 * @param citedRefs     the validated record IDs, as stored
 * @param citedRecords  the same records with dates and titles, for the ID chips
 * @param sources       what reflect used to word the warning
 */
public record WarningView(String id, String key, String kind, String foundBy, String severity, String summary,
                          List<String> citedRefs, List<RecordSummary> citedRecords, List<String> failedAttempts,
                          String priorFalsePositive, String recommendation, String status, Sources sources) {

    static WarningView of(Warning w, String foundBy, List<RecordSummary> records, Sources sources) {
        return new WarningView(w.id(), w.keyName(), w.kind(), foundBy, w.severity(), w.summary(), w.citedRefs(), records,
                w.failedAttempts(), w.priorFalsePositive(), w.recommendation(), w.status(), sources);
    }
}
