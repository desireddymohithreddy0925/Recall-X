package com.recallx.recallx.guard;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.memory.RecordIds;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.RecallHit;
import com.recallx.recallx.memory.hindsight.RecallRequest;
import com.recallx.recallx.memory.hindsight.ReflectAnswer;
import com.recallx.recallx.memory.hindsight.Sources;
import com.recallx.recallx.store.ClearedVerdict;
import com.recallx.recallx.store.ConfigChange;
import com.recallx.recallx.store.Decision;
import com.recallx.recallx.store.DecisionStore;
import com.recallx.recallx.store.Deployment;
import com.recallx.recallx.store.DeploymentStore;
import com.recallx.recallx.store.FixAttempt;
import com.recallx.recallx.store.IncidentStore;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.Warning;
import com.recallx.recallx.store.WarningStore;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Checks a config change against the team's history. The rule: the code decides with recall, people read reflect.
 * <ol>
 *   <li>If the newest verdict on this exact change (same key, same new value) was a false positive, the key gets a
 *       "previously judged safe" card instead of a warning. This is how a verdict changes what engineers see.</li>
 *   <li>An ACTIVE decision on the key always produces a Decision Guard warning, even with memory down.</li>
 *   <li>Otherwise a warning needs recall to return a real INC/ADR/WARN record above the score floor, reflect to say
 *       the history matches, and at least one incident that both recall and reflect point to.</li>
 *   <li>Reflect only words the warning. Every ID it cites is checked against MySQL, and severity comes from MySQL.</li>
 * </ol>
 * Keys are checked in parallel; warning IDs are assigned afterwards, in diff order.
 */
@Service
public class DecisionGuardService {

    static final Map<String, Object> WARNING_SCHEMA = warningSchema();

    private static final List<String> FACT_TYPES = List.of("world", "experience");
    private static final String DEFAULT_CLEARED_RECOMMENDATION = "Check that the reason above still holds before shipping.";

    private final DeploymentStore deployments;
    private final DecisionStore decisions;
    private final IncidentStore incidents;
    private final WarningStore warnings;
    private final RecordLookup lookup;
    private final HindsightClient memory;
    private final MemorySyncService memorySync;
    private final RecallxProperties.Hindsight settings;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "recallx-check");
        t.setDaemon(true);
        return t;
    });

    public DecisionGuardService(DeploymentStore deployments, DecisionStore decisions, IncidentStore incidents,
                                WarningStore warnings, RecordLookup lookup, HindsightClient memory,
                                MemorySyncService memorySync, RecallxProperties props) {
        this.deployments = deployments;
        this.decisions = decisions;
        this.incidents = incidents;
        this.warnings = warnings;
        this.lookup = lookup;
        this.memory = memory;
        this.memorySync = memorySync;
        this.settings = props.hindsight();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /** dryRun=true (used by the MCP tool) runs every rule but saves and retains nothing. */
    public CheckResponse check(String service, String version, String diff, boolean dryRun) {
        List<ConfigChange> changes = DiffParser.parse(diff);
        Deployment deployment = dryRun ? null : deployments.insert(service, version, changes);

        List<CompletableFuture<Outcome>> running = changes.stream()
                .map(change -> CompletableFuture.supplyAsync(() -> evaluate(service, change), executor))
                .toList();
        List<KeyResult> results = new ArrayList<>();
        for (CompletableFuture<Outcome> future : running) {
            Outcome outcome = join(future);
            results.add(outcome.pending() == null ? outcome.result() : save(outcome.pending(), deployment));
        }

        if (deployment != null) {
            memorySync.retainDeploymentLater(deployment.id());
        }
        return new CheckResponse(deployment == null ? null : deployment.id(), results);
    }

    // ------------------------------------------------------------------ one key

    Outcome evaluate(String service, ConfigChange change) {
        Optional<Decision> decision = decisions.activeForKey(change.keyName());
        Optional<ClearedVerdict> cleared = warnings.clearedFor(change.keyName(), change.newValue());

        // The recall check: without a decision or an earlier verdict, only continue if memory holds real history.
        Set<String> recalled = Set.of();
        if (decision.isEmpty() && cleared.isEmpty()) {
            List<RecallHit> hits;
            try {
                hits = memory.recall(RecallRequest.of(changeQuery(service, change), FACT_TYPES, "mid", 2048)
                        .withMinReranker(settings.minReranker())).hits();
            } catch (MemoryUnavailableException e) {
                return Outcome.of(KeyResult.memoryUnavailable(change));
            }
            Set<String> ids = new LinkedHashSet<>();
            hits.forEach(h -> ids.addAll(h.recordIds()));
            recalled = new LinkedHashSet<>(lookup.existingIds(ids).stream().filter(RecordIds::isEvidence).toList());
            if (recalled.isEmpty()) {
                return Outcome.of(KeyResult.noHistory(change));
            }
        }

        Draft draft;
        Sources sources;
        try {
            ReflectAnswer answer = reflectDraft(service, change, decision);
            draft = Draft.from(answer);
            sources = answer.basedOn().sources();
        } catch (MemoryUnavailableException e) {
            if (cleared.isEmpty() && decision.isEmpty()) {
                return Outcome.of(KeyResult.memoryUnavailable(change));
            }
            draft = cleared.isPresent() ? Draft.empty() : fallbackDraft(decision.get(), change);
            sources = Sources.none();
        }
        List<String> reflectCited = lookup.existingIds(draft.referencedIds());
        String priorFalsePositive = relevantPriorFalsePositive(draft.priorFalsePositive(), change.keyName());

        if (cleared.isPresent()) {
            return Outcome.of(KeyResult.cleared(change, clearedView(cleared.get(), decision, draft, reflectCited, sources)));
        }

        if (decision.isPresent()) {
            Decision d = decision.get();
            Set<String> cited = new LinkedHashSet<>();
            cited.add(d.id());
            if (d.linkedIncidentId() != null) cited.add(d.linkedIncidentId());
            cited.addAll(reflectCited);
            String severity = lookup.severityOf(d.linkedIncidentId()).orElse(null);
            return Outcome.pending(new PendingWarning(change, "DECISION_GUARD", "DECISION", severity, draft,
                    lookup.existingIds(cited), failedAttempts(List.of(d.linkedIncidentId())), priorFalsePositive, sources));
        }

        // A memory-only warning: reflect must say the history matches, and recall and reflect must agree on an incident.
        if (Boolean.FALSE.equals(draft.matchesHistory())) {
            return Outcome.of(KeyResult.noHistory(change));
        }
        Set<String> found = recalled;
        List<String> agreed = reflectCited.stream().filter(found::contains).filter(RecordIds::isIncident).toList();
        if (agreed.isEmpty()) {
            return Outcome.of(KeyResult.noHistory(change));
        }
        return Outcome.pending(new PendingWarning(change, "HISTORY", "MEMORY", lookup.worstSeverity(agreed), draft,
                reflectCited, failedAttempts(agreed), priorFalsePositive, sources));
    }

    /**
     * Failed fixes come from MySQL, for the incidents the warning is based on. Reflect's own list tends to mix in
     * attempts from other incidents it read along the way.
     */
    private List<String> failedAttempts(List<String> incidentIds) {
        List<String> failed = new ArrayList<>();
        for (String id : incidentIds) {
            if (id == null) continue;
            for (FixAttempt a : incidents.attempts(id)) {
                if ("FAILED".equals(a.outcome())) failed.add(a.action() + " (" + id + ")");
            }
        }
        return failed;
    }

    /**
     * Directive 4 makes reflect mention any earlier false positive it finds, including ones about other settings.
     * Keep the note only if it names a false-positive warning on this same key.
     */
    private String relevantPriorFalsePositive(String note, String keyName) {
        if (note == null || note.isBlank()) return "";
        boolean sameKey = RecordIds.extract(note).stream()
                .filter(id -> id.startsWith("WARN-"))
                .map(warnings::findById)
                .flatMap(Optional::stream)
                .anyMatch(w -> keyName.equals(w.keyName()) && "FALSE_POSITIVE".equals(w.status()));
        return sameKey ? note : "";
    }

    /** Saves a warning under the next WARN-n ID (unless this is a dry run) and turns it into the view. */
    private KeyResult save(PendingWarning p, Deployment deployment) {
        List<String> cited = p.citedRefs().stream()
                .filter(id -> deployment == null || !id.equals(deployment.id()))
                .toList();
        Warning warning = new Warning(null, deployment == null ? null : deployment.id(), p.change().keyName(),
                Instant.now(), p.kind(), p.severity(), p.draft().summary(), cited, p.failedAttempts(),
                p.priorFalsePositive(), p.draft().recommendation(), "PENDING", null, null);
        if (deployment != null) {
            warning = warnings.insertWithNextId(warning);
        }
        return KeyResult.warning(p.change(), WarningView.of(warning, p.foundBy(), lookup.describe(cited), p.sources()));
    }

    private ClearedView clearedView(ClearedVerdict verdict, Optional<Decision> decision, Draft draft,
                                    List<String> reflectCited, Sources sources) {
        String date = verdict.createdAt().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        StringBuilder facts = new StringBuilder("Previously judged safe in ").append(verdict.warningId())
                .append(" (").append(date).append("): ").append(verdict.reason() == null ? "" : verdict.reason().trim());
        decision.ifPresent(d -> facts.append(' ').append(d.id()).append(" still applies: ").append(d.decision()));
        String summary = draft.summary().isBlank() ? facts.toString() : draft.summary();
        String recommendation = draft.recommendation().isBlank() ? DEFAULT_CLEARED_RECOMMENDATION : draft.recommendation();

        Set<String> cited = new LinkedHashSet<>();
        cited.add(verdict.warningId());
        decision.ifPresent(d -> {
            cited.add(d.id());
            if (d.linkedIncidentId() != null) cited.add(d.linkedIncidentId());
        });
        cited.addAll(reflectCited);
        return new ClearedView(verdict.warningId(), date, verdict.reason(),
                decision.map(Decision::id).orElse(null), decision.map(Decision::decision).orElse(null),
                summary, recommendation, lookup.describe(cited), sources);
    }

    private ReflectAnswer reflectDraft(String service, ConfigChange change, Optional<Decision> decision) {
        String prompt = warningPrompt(service, change, decision);
        ReflectAnswer answer = memory.reflect(prompt, settings.checkBudget(), WARNING_SCHEMA);
        if (answer.structuredOutput() == null && !answer.structuredOutputError().isBlank()) {
            answer = memory.reflect(prompt, settings.checkBudget(), WARNING_SCHEMA);   // retry once
        }
        return answer;
    }

    /** The recall query describes the change itself. Generic words like "incidents" would match every incident. */
    static String changeQuery(String service, ConfigChange change) {
        return service + ": change " + change.keyName() + " from " + change.oldValue() + " to " + change.newValue();
    }

    static String warningPrompt(String service, ConfigChange change, Optional<Decision> decision) {
        StringBuilder sb = new StringBuilder();
        sb.append("A deployment to ").append(service).append(" changes ").append(change.keyName())
                .append(" from ").append(change.oldValue()).append(" to ").append(change.newValue()).append(".\n");
        decision.ifPresent(d -> sb.append("This key is governed by ").append(d.id()).append(": ")
                .append(d.decision()).append('\n'));
        sb.append("Using this team's history, explain whether this change resembles past incidents or undoes a past decision. ")
                .append("Name any fixes that failed before, and any earlier warning on a similar change that was marked ")
                .append("a false positive, with its reason. Cite IDs for every claim. ")
                .append("Describe similarity; do not predict that anything will fail.");
        return sb.toString();
    }

    /** Built from the database alone, used when memory or the model is unavailable. */
    private Draft fallbackDraft(Decision d, ConfigChange change) {
        String incidentNote = d.linkedIncidentId() == null ? "" : " Linked incident: " + d.linkedIncidentId() + ".";
        String summary = change.keyName() + " is set by " + d.id() + " (" + d.decidedAt().toString().substring(0, 10)
                + "): " + d.decision() + " Reason: " + d.reason() + incidentNote;
        return new Draft(summary, List.of(), List.of(), "",
                "Keep the current value, or record a new decision before changing it.", null);
    }

    private static Outcome join(CompletableFuture<Outcome> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }

    private static Map<String, Object> warningSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("matches_history", Map.of("type", "boolean", "description",
                "true only if a cited record involves the same setting, component or failure mode as this change"));
        properties.put("summary", Map.of("type", "string", "description", "2-3 sentences, citing record IDs"));
        properties.put("cited_ids", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Every INC-, ADR-, DEP- or WARN- ID used"));
        properties.put("failed_attempts", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Fixes that were tried before and did not work"));
        properties.put("prior_false_positive", Map.of("type", "string",
                "description", "An earlier similar warning marked false positive, and why; empty if none"));
        properties.put("recommendation", Map.of("type", "string",
                "description", "One sentence on what to check before shipping"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("matches_history", "summary", "cited_ids", "failed_attempts", "recommendation"));
        return schema;
    }

    // ------------------------------------------------------------------ internal types

    /** Either a final result, or a warning waiting for its ID. */
    record Outcome(KeyResult result, PendingWarning pending) {
        static Outcome of(KeyResult result) {
            return new Outcome(result, null);
        }

        static Outcome pending(PendingWarning pending) {
            return new Outcome(null, pending);
        }
    }

    record PendingWarning(ConfigChange change, String kind, String foundBy, String severity, Draft draft,
                          List<String> citedRefs, List<String> failedAttempts, String priorFalsePositive,
                          Sources sources) { }

    /** What reflect wrote. matchesHistory is null when reflect gave no structured output. */
    record Draft(String summary, List<String> citedIds, List<String> failedAttempts,
                 String priorFalsePositive, String recommendation, Boolean matchesHistory) {

        static Draft empty() {
            return new Draft("", List.of(), List.of(), "", "", null);
        }

        static Draft from(ReflectAnswer a) {
            Map<String, Object> s = a.structuredOutput();
            if (s == null) {
                return new Draft(a.text(), new ArrayList<>(RecordIds.extract(a.text())), List.of(), "", "", null);
            }
            String summary = str(s.get("summary"));
            Boolean matches = s.get("matches_history") instanceof Boolean b ? b : null;
            return new Draft(summary.isBlank() ? a.text() : summary, strings(s.get("cited_ids")),
                    strings(s.get("failed_attempts")), str(s.get("prior_false_positive")), str(s.get("recommendation")),
                    matches);
        }

        /**
         * The records reflect's summary actually talks about. In practice cited_ids lists everything reflect read,
         * relevant or not, so it's only used when the summary names no records. When reflect says the history
         * doesn't match, nothing counts: the summary often names an incident while explaining why it doesn't apply.
         */
        Set<String> referencedIds() {
            if (Boolean.FALSE.equals(matchesHistory)) return Set.of();
            Set<String> ids = RecordIds.extract(summary);
            return ids.isEmpty() ? new LinkedHashSet<>(citedIds) : ids;
        }

        private static String str(Object o) {
            return o == null ? "" : o.toString();
        }

        private static List<String> strings(Object o) {
            List<String> out = new ArrayList<>();
            if (o instanceof List<?> list) {
                for (Object item : list) if (item != null && !item.toString().isBlank()) out.add(item.toString().trim());
            }
            return out;
        }
    }
}
