package com.recallx.recallx.memory.hindsight;

import java.util.List;

/**
 * A recall query.
 *
 * @param types              "world", "experience", "observation"; empty for all types
 * @param minReranker        drop results whose reranker score is below this (Hindsight's min_scores); null for no floor
 * @param includeSourceFacts for observations: also return the facts each observation was built from
 */
public record RecallRequest(String query, List<String> types, String budget, int maxTokens,
                            Double minReranker, boolean includeSourceFacts) {

    public RecallRequest {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public static RecallRequest of(String query, List<String> types, String budget, int maxTokens) {
        return new RecallRequest(query, types, budget, maxTokens, null, false);
    }

    public RecallRequest withMinReranker(double floor) {
        return new RecallRequest(query, types, budget, maxTokens, floor, includeSourceFacts);
    }

    public RecallRequest withSourceFacts() {
        return new RecallRequest(query, types, budget, maxTokens, minReranker, true);
    }
}
