package com.recallx.recallx.memory;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.RetainItem;
import com.recallx.recallx.store.Decision;
import com.recallx.recallx.store.DecisionStore;
import com.recallx.recallx.store.Deployment;
import com.recallx.recallx.store.DeploymentStore;
import com.recallx.recallx.store.Incident;
import com.recallx.recallx.store.IncidentStore;
import com.recallx.recallx.store.Warning;
import com.recallx.recallx.store.WarningStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Copies database records into Hindsight memory (retain). The database is the system of record;
 * retained_at marks what has already been sent, so running the sync twice never duplicates memories.
 * Each record is retained under its own ID as document_id, so re-retaining it replaces the old version.
 */
@Service
public class MemorySyncService {

    private static final Logger log = LoggerFactory.getLogger(MemorySyncService.class);
    private static final int BATCH = 10;

    private final HindsightClient memory;
    private final BankSetup bankSetup;
    private final IncidentStore incidents;
    private final DecisionStore decisions;
    private final DeploymentStore deployments;
    private final WarningStore warnings;
    private final RecallxProperties props;

    public MemorySyncService(HindsightClient memory, BankSetup bankSetup, IncidentStore incidents,
                             DecisionStore decisions, DeploymentStore deployments, WarningStore warnings,
                             RecallxProperties props) {
        this.memory = memory;
        this.bankSetup = bankSetup;
        this.incidents = incidents;
        this.decisions = decisions;
        this.deployments = deployments;
        this.warnings = warnings;
        this.props = props;
    }

    public record SyncReport(int incidents, int decisions, int deployments, int warningOutcomes, List<String> bankSetup) { }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!props.seedMemoryOnStartup()) return;
        try {
            SyncReport report = syncAll();
            log.info("[memory] startup sync done: {}", report);
        } catch (MemoryUnavailableException e) {
            log.warn("[memory] startup sync failed; run POST /api/admin/memory/sync later: {}", e.getMessage());
        }
    }

    /**
     * Retains every record not yet in memory, in batches, asynchronously on the Hindsight side, then applies the
     * bank settings. Retain comes first because the first retain creates the bank.
     */
    public SyncReport syncAll() {
        List<Pending> pending = new ArrayList<>();
        List<Incident> newIncidents = incidents.pendingRetain();
        newIncidents.forEach(i -> pending.add(new Pending(incidentItem(i), () -> incidents.markRetained(i.id()))));
        List<Decision> newDecisions = decisions.pendingRetain();
        newDecisions.forEach(d -> pending.add(new Pending(decisionItem(d), () -> decisions.markRetained(d.id()))));
        List<Deployment> newDeployments = deployments.trackedPendingRetain();
        newDeployments.forEach(d -> pending.add(new Pending(deploymentItem(d), () -> deployments.markRetained(d.id()))));
        List<Warning> newOutcomes = warnings.pendingRetain();
        newOutcomes.forEach(w -> pending.add(new Pending(warningItem(w), () -> warnings.markRetained(w.id()))));

        for (int start = 0; start < pending.size(); start += BATCH) {
            List<Pending> batch = pending.subList(start, Math.min(start + BATCH, pending.size()));
            memory.retain(batch.stream().map(Pending::item).toList(), true);
            batch.forEach(p -> p.markRetained().run());
        }
        List<String> setup = bankSetup.apply();
        return new SyncReport(newIncidents.size(), newDecisions.size(), newDeployments.size(), newOutcomes.size(), setup);
    }

    /** Retains one warning verdict. Returns false if memory is unavailable; the sync endpoint can retry later. */
    public boolean retainWarningOutcome(String warningId) {
        return warnings.findById(warningId).map(w -> retainOne(warningItem(w), () -> warnings.markRetained(w.id())))
                .orElse(false);
    }

    public boolean retainIncident(String incidentId) {
        return incidents.findById(incidentId).map(i -> retainOne(incidentItem(i), () -> incidents.markRetained(i.id())))
                .orElse(false);
    }

    /** Retains a live deployment in the background, after the check response has been sent. */
    public void retainDeploymentLater(String deploymentId) {
        CompletableFuture.runAsync(() -> deployments.findById(deploymentId)
                .ifPresent(d -> retainOne(deploymentItem(d), () -> deployments.markRetained(d.id()))));
    }

    /** Best effort: removes documents from memory (used by the demo reset). Returns how many were removed. */
    public int forget(Collection<String> documentIds) {
        int removed = 0;
        for (String id : documentIds) {
            try {
                memory.deleteDocument(id);
                removed++;
            } catch (MemoryUnavailableException e) {
                log.warn("[memory] could not delete document {}: {}", id, e.getMessage());
            }
        }
        return removed;
    }

    private boolean retainOne(RetainItem item, Runnable markRetained) {
        try {
            memory.retain(List.of(item), true);
            markRetained.run();
            return true;
        } catch (MemoryUnavailableException e) {
            log.warn("[memory] could not retain {}: {}", item.documentId(), e.getMessage());
            return false;
        }
    }

    // Timestamps are when the event happened, not when it was loaded: this is what makes "three months ago" work.

    private RetainItem incidentItem(Incident i) {
        String text = MemoryTemplates.incident(i, incidents.attempts(i.id()), incidents.keys(i.id()));
        return new RetainItem(text, "incident", i.startedAt(), i.id(),
                Map.of("type", "incident", "ref", i.id(), "service", i.serviceId()));
    }

    private RetainItem decisionItem(Decision d) {
        return new RetainItem(MemoryTemplates.decision(d), "decision", d.decidedAt(), d.id(),
                Map.of("type", "decision", "ref", d.id(), "service", d.serviceId()));
    }

    private RetainItem deploymentItem(Deployment d) {
        String text = MemoryTemplates.deployment(d, deployments.changes(d.id()));
        return new RetainItem(text, "deployment", d.deployedAt(), d.id(),
                Map.of("type", "deployment", "ref", d.id(), "service", d.serviceId()));
    }

    private RetainItem warningItem(Warning w) {
        return new RetainItem(MemoryTemplates.warningOutcome(w), "warning_outcome",
                w.decidedAt() != null ? w.decidedAt() : w.createdAt(), w.id(),
                Map.of("type", "warning_outcome", "ref", w.id(), "verdict", w.status()));
    }

    private record Pending(RetainItem item, Runnable markRetained) { }
}
