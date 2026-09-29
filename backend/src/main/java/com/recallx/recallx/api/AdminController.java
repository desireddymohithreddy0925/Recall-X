package com.recallx.recallx.api;

import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.RecallHit;
import com.recallx.recallx.memory.hindsight.RecallRequest;
import com.recallx.recallx.service.DemoResetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Setup and demo helpers. Everything here needs the X-Admin-Token header (see AdminTokenFilter): these calls
 * spend Hindsight credits or delete data.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MemorySyncService memorySync;
    private final HindsightClient memory;
    private final DemoResetService reset;

    public AdminController(MemorySyncService memorySync, HindsightClient memory, DemoResetService reset) {
        this.memorySync = memorySync;
        this.memory = memory;
        this.reset = reset;
    }

    /** One recall result: the record it came from, and how relevant Hindsight scored it for this query. */
    public record Hit(String recordId, Double reranker, String type, String text) {
        static Hit of(RecallHit h) {
            List<String> ids = List.copyOf(h.recordIds());
            return new Hit(ids.isEmpty() ? null : ids.get(0), h.reranker(), h.type(), h.text());
        }
    }

    public record RecallCheck(String bankId, String query, int hits, List<String> recordIds, List<Hit> results) { }

    /** Retains every record not yet in memory, then applies the bank settings. Safe to run more than once. */
    @PostMapping("/memory/sync")
    public MemorySyncService.SyncReport sync() {
        return memorySync.syncAll();
    }

    /** Is the seed searchable yet? e.g. GET /api/admin/memory/check?q=payment-service maximum-pool-size */
    @GetMapping("/memory/check")
    public RecallCheck check(@RequestParam(defaultValue = "payment-service maximum-pool-size") String q) {
        List<Hit> hits = memory.recall(RecallRequest.of(q, List.of(), "low", 2048)).hits().stream().map(Hit::of).toList();
        List<String> ids = hits.stream().map(Hit::recordId).filter(id -> id != null).distinct().toList();
        return new RecallCheck(memory.bankId(), q, hits.size(), ids, hits);
    }

    /** Deletes everything created after the seed, so the demo starts again from DEP-2026-061 and WARN-55. */
    @PostMapping("/demo/reset")
    public DemoResetService.ResetReport resetDemo() {
        return reset.reset();
    }
}
