package com.recallx.recallx.memory.hindsight;

import com.recallx.recallx.memory.RecordIds;

import java.util.List;
import java.util.Set;

/**
 * One memory returned by recall.
 *
 * @param type          "world", "experience" or "observation"
 * @param documentId    the document it was retained under; RECALL-X uses the record ID (INC-18, ADR-7, ...)
 * @param reranker      0-1 relevance from the cross-encoder, relative to this query; null if not returned
 * @param sourceFactIds for observations: the facts it was consolidated from
 */
public record RecallHit(String id, String text, String type, String documentId, String occurredStart,
                        Double reranker, Double finalScore, List<String> sourceFactIds) {

    public RecallHit {
        text = text == null ? "" : text;
        sourceFactIds = sourceFactIds == null ? List.of() : List.copyOf(sourceFactIds);
    }

    /** Convenience for tests and simple callers. */
    public static RecallHit of(String documentId, String text, double reranker) {
        return new RecallHit("f-" + documentId, text, "world", documentId, null, reranker, reranker, List.of());
    }

    /** The record this hit came from: its document_id when that is a record ID, otherwise any record IDs in its text. */
    public Set<String> recordIds() {
        if (documentId != null && RecordIds.isRecordId(documentId)) {
            return Set.of(documentId);
        }
        return RecordIds.extract(text);
    }
}
