package com.recallx.recallx.service;

import com.recallx.recallx.memory.RecordIds;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.RecallHit;
import com.recallx.recallx.memory.hindsight.RecallRequest;
import com.recallx.recallx.memory.hindsight.RecallResponse;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Recurring patterns are Hindsight observations: beliefs Hindsight consolidated from several memories on its own.
 * Each one is shown with the records it came from, and every record ID is checked against MySQL.
 */
@Service
public class PatternService {

    private static final int MAX_PATTERNS = 5;
    private static final Duration CACHE_FOR = Duration.ofSeconds(60);

    private final HindsightClient memory;
    private final RecordLookup lookup;
    private volatile Cached cached;

    public PatternService(HindsightClient memory, RecordLookup lookup) {
        this.memory = memory;
        this.lookup = lookup;
    }

    public record Pattern(String text, List<RecordSummary> records) { }

    public record PatternsResponse(List<Pattern> patterns, boolean memoryAvailable) { }

    private record Cached(String service, Instant at, PatternsResponse response) { }

    public PatternsResponse patterns(String service) {
        Cached c = cached;
        if (c != null && c.service().equals(service) && c.at().plus(CACHE_FOR).isAfter(Instant.now())) {
            return c.response();
        }
        PatternsResponse response;
        try {
            RecallResponse recall = memory.recall(RecallRequest.of("recurring failure patterns in " + service,
                    List.of("observation"), "low", 1024).withSourceFacts());
            List<Pattern> patterns = new ArrayList<>();
            for (RecallHit observation : recall.hits()) {
                if (observation.text().isBlank()) continue;
                Set<String> ids = new LinkedHashSet<>();
                for (String factId : observation.sourceFactIds()) {
                    RecallHit fact = recall.sourceFacts().get(factId);
                    if (fact != null) ids.addAll(fact.recordIds());
                }
                ids.addAll(RecordIds.extract(observation.text()));
                patterns.add(new Pattern(observation.text(), lookup.describe(ids)));
                if (patterns.size() == MAX_PATTERNS) break;
            }
            response = new PatternsResponse(patterns, true);
        } catch (MemoryUnavailableException e) {
            return new PatternsResponse(List.of(), false);   // not cached, so the next call tries again
        }
        cached = new Cached(service, Instant.now(), response);
        return response;
    }
}
