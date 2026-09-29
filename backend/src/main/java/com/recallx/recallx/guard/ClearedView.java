package com.recallx.recallx.guard;

import com.recallx.recallx.memory.hindsight.Sources;
import com.recallx.recallx.store.RecordSummary;

import java.util.List;

/**
 * "Previously judged safe": the same change (same key, same new value) was warned about before, and an engineer
 * marked that warning a false positive. The facts come from MySQL; summary is reflect's wording when memory is up.
 * Not saved as a warning, so it has no verdict buttons and doesn't count toward precision.
 *
 * @param warningId  the earlier warning, e.g. WARN-42
 * @param date       when it was raised, yyyy-MM-dd
 * @param reason     the engineer's reason for the false positive
 * @param decisionId an active decision that still governs the key, or null
 */
public record ClearedView(String warningId, String date, String reason, String decisionId, String decisionText,
                          String summary, String recommendation, List<RecordSummary> citedRecords, Sources sources) {
}
