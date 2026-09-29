package com.recallx.recallx.service;

import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.store.ResetStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Puts the demo back to its starting state: deletes every row created after the seed, then removes the matching
 * documents from memory. The next run reuses the same IDs (DEP-2026-061, WARN-55), and retaining a document ID
 * again replaces the old memory, so rehearsals don't pile up in Hindsight either way.
 */
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final ResetStore resetStore;
    private final MemorySyncService memorySync;

    public DemoResetService(ResetStore resetStore, MemorySyncService memorySync) {
        this.resetStore = resetStore;
        this.memorySync = memorySync;
    }

    public record ResetReport(List<String> deletedRecords, int documentsRemovedFromMemory) { }

    public ResetReport reset() {
        List<String> deleted = resetStore.deleteUnseeded();
        int removed = memorySync.forget(deleted);
        log.info("[reset] deleted {} live record(s) {}; removed {} document(s) from memory", deleted.size(), deleted, removed);
        return new ResetReport(deleted, removed);
    }
}
