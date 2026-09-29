package com.recallx.recallx.memory.hindsight;

import java.util.List;
import java.util.Map;

/** Recall results, plus the source facts of any observations when they were requested (keyed by fact ID). */
public record RecallResponse(List<RecallHit> hits, Map<String, RecallHit> sourceFacts) {

    public RecallResponse {
        hits = hits == null ? List.of() : List.copyOf(hits);
        sourceFacts = sourceFacts == null ? Map.of() : Map.copyOf(sourceFacts);
    }
}
