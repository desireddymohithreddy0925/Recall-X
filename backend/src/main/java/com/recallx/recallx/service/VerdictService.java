package com.recallx.recallx.service;

import com.recallx.recallx.common.BadRequestException;
import com.recallx.recallx.common.ConflictException;
import com.recallx.recallx.common.NotFoundException;
import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.store.WarningStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/** The learning loop: every verdict is saved and retained as memory, so the next similar check knows about it. */
@Service
public class VerdictService {

    private static final Set<String> VERDICTS = Set.of("USEFUL", "FALSE_POSITIVE", "IGNORED");

    private final WarningStore warnings;
    private final MemorySyncService memorySync;

    public VerdictService(WarningStore warnings, MemorySyncService memorySync) {
        this.warnings = warnings;
        this.memorySync = memorySync;
    }

    public record VerdictResponse(String id, String status, boolean retained) { }

    public VerdictResponse record(String warningId, String verdict, String reason) {
        if (verdict == null || !VERDICTS.contains(verdict)) {
            throw new BadRequestException("verdict must be USEFUL, FALSE_POSITIVE or IGNORED");
        }
        if ("FALSE_POSITIVE".equals(verdict) && (reason == null || reason.isBlank())) {
            throw new BadRequestException("A false positive needs a reason, so RECALL-X can learn from it");
        }
        warnings.findById(warningId).orElseThrow(() -> new NotFoundException("No warning " + warningId));
        int updated = warnings.recordVerdict(warningId, verdict, reason == null ? null : reason.trim(), Instant.now());
        if (updated == 0) {
            throw new ConflictException(warningId + " already has a verdict");
        }
        boolean retained = memorySync.retainWarningOutcome(warningId);
        return new VerdictResponse(warningId, verdict, retained);
    }
}
