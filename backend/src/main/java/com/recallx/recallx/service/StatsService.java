package com.recallx.recallx.service;

import com.recallx.recallx.store.Counts;
import com.recallx.recallx.store.MonthPrecision;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.Warning;
import com.recallx.recallx.store.WarningStore;
import org.springframework.stereotype.Service;

import java.util.List;

/** Every number on screen comes from the database, never from the model. */
@Service
public class StatsService {

    private final RecordLookup lookup;
    private final WarningStore warnings;

    public StatsService(RecordLookup lookup, WarningStore warnings) {
        this.lookup = lookup;
        this.warnings = warnings;
    }

    public record WarningSummary(String id, String deploymentId, String key, String kind, String severity,
                                 String status, String createdAt, String summary) {
        static WarningSummary of(Warning w) {
            return new WarningSummary(w.id(), w.deploymentId(), w.keyName(), w.kind(), w.severity(), w.status(),
                    w.createdAt() == null ? null : w.createdAt().toString(), w.summary());
        }
    }

    public record StatsResponse(Counts counts, List<MonthPrecision> precisionByMonth, List<WarningSummary> recentWarnings) { }

    public StatsResponse stats() {
        return new StatsResponse(lookup.counts(), warnings.precisionByMonth(),
                warnings.list(null, 8).stream().map(WarningSummary::of).toList());
    }

    public List<WarningSummary> warnings(String status, int limit) {
        return warnings.list(status, limit).stream().map(WarningSummary::of).toList();
    }
}
