package com.recallx.recallx.guard;

import com.recallx.recallx.TestProperties;
import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.memory.hindsight.BasedOn;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.RecallHit;
import com.recallx.recallx.memory.hindsight.RecallRequest;
import com.recallx.recallx.memory.hindsight.RecallResponse;
import com.recallx.recallx.memory.hindsight.ReflectAnswer;
import com.recallx.recallx.store.ClearedVerdict;
import com.recallx.recallx.store.Decision;
import com.recallx.recallx.store.DecisionStore;
import com.recallx.recallx.store.Deployment;
import com.recallx.recallx.store.DeploymentStore;
import com.recallx.recallx.store.FixAttempt;
import com.recallx.recallx.store.IncidentStore;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import com.recallx.recallx.store.Warning;
import com.recallx.recallx.store.WarningStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecisionGuardServiceTest {

    private static final String SERVICE = "payment-service";
    private static final String POOL = "spring.datasource.hikari.maximum-pool-size";
    private static final String RETRY = "payment.retry.max-attempts";
    private static final String CRON = "payment.settlement.cron";
    private static final String LOG = "logging.level.com.acmepay";
    private static final Set<String> IN_DATABASE =
            Set.of("ADR-7", "ADR-9", "ADR-11", "INC-13", "INC-18", "INC-29", "INC-33", "WARN-42", "DEP-2026-017");

    private final DeploymentStore deployments = mock(DeploymentStore.class);
    private final DecisionStore decisions = mock(DecisionStore.class);
    private final IncidentStore incidents = mock(IncidentStore.class);
    private final WarningStore warnings = mock(WarningStore.class);
    private final RecordLookup lookup = mock(RecordLookup.class);
    private final HindsightClient memory = mock(HindsightClient.class);
    private final MemorySyncService memorySync = mock(MemorySyncService.class);

    private final DecisionGuardService guard = new DecisionGuardService(deployments, decisions, incidents, warnings,
            lookup, memory, memorySync, TestProperties.props());

    private final Decision adr7 = new Decision("ADR-7", SERVICE, POOL, Instant.parse("2026-03-15T11:00:00Z"),
            "Keep maximum-pool-size at 20 per replica.",
            "After INC-18, replicas x pool size must stay below MySQL max_connections of 150.", "ACTIVE", "INC-18");
    private final Decision adr9 = new Decision("ADR-9", SERVICE, RETRY, Instant.parse("2026-04-06T11:00:00Z"),
            "Retries above 3 require payment.retry.idempotency-enabled to be true.",
            "INC-29: retries without idempotency caused duplicate charges.", "ACTIVE", "INC-29");
    private final Decision adr11 = new Decision("ADR-11", SERVICE, CRON, Instant.parse("2026-09-03T11:00:00Z"),
            "Settlement runs at 01:30 and never during peak hours.", "INC-33.", "ACTIVE", "INC-33");

    @BeforeEach
    void databaseKnowsOnlySomeRecords() {
        when(lookup.existingIds(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter(IN_DATABASE::contains).distinct().toList();
        });
        when(lookup.describe(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter(IN_DATABASE::contains).distinct()
                    .map(id -> new RecordSummary(id, "INCIDENT", "2026-03-12", null, "title")).toList();
        });
        when(lookup.severityOf("INC-18")).thenReturn(Optional.of("SEV2"));
        when(lookup.severityOf("INC-29")).thenReturn(Optional.of("SEV1"));
        when(lookup.severityOf("INC-33")).thenReturn(Optional.of("SEV2"));
        when(lookup.worstSeverity(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            return ids.contains("INC-29") ? "SEV1" : ids.contains("INC-13") ? "SEV3" : "SEV2";
        });
        when(incidents.attempts("INC-18")).thenReturn(List.of(
                new FixAttempt(1, "Restarted payment-service pods", "FAILED", "Timeouts returned within 10 minutes"),
                new FixAttempt(3, "Reverted maximum-pool-size to 20", "RESOLVED", null)));
    }

    // ------------------------------------------------------------------ Decision Guard

    @Test
    void decisionGuardFiresEvenWhenMemoryIsDown() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        reflectFails();

        KeyResult result = check(POOL + ": 20 -> 60").get(0);

        assertThat(result.status()).isEqualTo("WARNING");
        assertThat(result.warning().kind()).isEqualTo("DECISION_GUARD");
        assertThat(result.warning().foundBy()).isEqualTo("DECISION");
        assertThat(result.warning().citedRefs()).containsExactly("ADR-7", "INC-18");
        assertThat(result.warning().severity()).isEqualTo("SEV2");
        assertThat(result.warning().failedAttempts()).anyMatch(a -> a.startsWith("Restarted payment-service pods"));
        verify(memory, never()).recall(any(RecallRequest.class));
    }

    @Test
    void decisionGuardSeverityComesFromTheLinkedIncidentNotFromEverythingReflectMentions() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        reflectReturns(structured(true, "Resembles INC-18. INC-29 was a different failure.", List.of("INC-18", "INC-29")));

        KeyResult result = check(POOL + ": 20 -> 60").get(0);

        assertThat(result.warning().severity()).isEqualTo("SEV2");   // INC-18, even though INC-29 (SEV1) is cited
        assertThat(result.warning().citedRefs()).contains("ADR-7", "INC-18", "INC-29");
    }

    @Test
    void citationsComeFromWhatTheSummarySaysNotEverythingReflectRead() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        reflectReturns(structured(true, "Raising the pool violates ADR-7 and mirrors INC-18.",
                List.of("ADR-7", "INC-18", "DEP-2026-017", "WARN-42", "INC-29"), List.of(), ""));

        KeyResult result = check(POOL + ": 20 -> 60").get(0);

        assertThat(result.warning().citedRefs()).containsExactly("ADR-7", "INC-18");
    }

    @Test
    void failedFixesComeFromTheDatabaseForTheIncidentBehindTheWarning() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        reflectReturns(structured(true, "Mirrors INC-18.", List.of("INC-18"),
                List.of("Restarting pods", "Lowering the gateway timeout"), ""));

        KeyResult result = check(POOL + ": 20 -> 60").get(0);

        assertThat(result.warning().failedAttempts()).containsExactly("Restarted payment-service pods (INC-18)");
    }

    @Test
    void anEarlierFalsePositiveAboutAnotherSettingIsNotShown() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        when(warnings.findById("WARN-42")).thenReturn(Optional.of(warn42()));
        reflectReturns(structured(true, "Mirrors INC-18.", List.of("INC-18"), List.of(),
                "WARN-42 was a false positive because idempotency was on."));

        assertThat(check(POOL + ": 20 -> 60").get(0).warning().priorFalsePositive()).isEmpty();
    }

    @Test
    void anEarlierFalsePositiveOnTheSameSettingIsShown() {
        when(decisions.activeForKey(RETRY)).thenReturn(Optional.of(adr9));
        when(warnings.findById("WARN-42")).thenReturn(Optional.of(warn42()));
        reflectReturns(structured(true, "Governed by ADR-9; resembles INC-29.", List.of("ADR-9", "INC-29"), List.of(),
                "WARN-42 was a false positive because idempotency was on."));

        KeyResult result = check(RETRY + ": 3 -> 5").get(0);   // a different new value, so not "previously judged safe"

        assertThat(result.status()).isEqualTo("WARNING");
        assertThat(result.warning().priorFalsePositive()).startsWith("WARN-42 was a false positive");
    }

    @Test
    void memoryDownWithoutADecisionIsUnavailableNeverNoHistory() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        when(memory.recall(any(RecallRequest.class))).thenThrow(new MemoryUnavailableException("down", 503, null));
        reflectFails();

        List<KeyResult> results = check(POOL + ": 20 -> 60\n" + LOG + ": INFO -> DEBUG");

        assertThat(results.get(0).status()).isEqualTo("WARNING");
        assertThat(results.get(1).status()).isEqualTo("MEMORY_UNAVAILABLE");
    }

    // ------------------------------------------------------------------ the recall check

    @Test
    void recallAsksAboutTheChangeItselfWithTheScoreFloor() {
        recallReturns();

        check(LOG + ": INFO -> DEBUG");

        ArgumentCaptor<RecallRequest> request = ArgumentCaptor.forClass(RecallRequest.class);
        verify(memory).recall(request.capture());
        assertThat(request.getValue().query()).isEqualTo("payment-service: change " + LOG + " from INFO to DEBUG");
        assertThat(request.getValue().minReranker()).isEqualTo(TestProperties.FLOOR);
        assertThat(request.getValue().types()).containsExactly("world", "experience");
    }

    @Test
    void nothingAboveTheFloorMeansNoHistoryWithoutCallingReflect() {
        recallReturns();

        KeyResult result = check(LOG + ": INFO -> DEBUG").get(0);

        assertThat(result.status()).isEqualTo("NO_HISTORY");
        verify(memory, never()).reflect(anyString(), anyString(), any());
    }

    @Test
    void aPastDeploymentAloneIsNotHistory() {
        recallReturns(RecallHit.of("DEP-2026-017", "Deployment changed several settings", 0.9));

        assertThat(check(LOG + ": INFO -> DEBUG").get(0).status()).isEqualTo("NO_HISTORY");
    }

    @Test
    void theRecallCheckUsesDocumentIdsEvenWhenTheFactTextHasNoId() {
        recallReturns(RecallHit.of("INC-13", "Debug logging left on filled the log volume", 0.8));
        reflectReturns(structured(true, "Turning on DEBUG resembles INC-13, when debug logging filled a disk.",
                List.of("INC-13")));

        KeyResult result = check(LOG + ": INFO -> DEBUG").get(0);

        assertThat(result.status()).isEqualTo("WARNING");
        assertThat(result.warning().kind()).isEqualTo("HISTORY");
        assertThat(result.warning().foundBy()).isEqualTo("MEMORY");
        assertThat(result.warning().severity()).isEqualTo("SEV3");
        assertThat(result.warning().citedRefs()).containsExactly("INC-13");
    }

    @Test
    void reflectSayingTheHistoryDoesNotMatchMeansNoHistoryEvenIfItNamesAnIncident() {
        recallReturns(RecallHit.of("INC-13", "Debug logging filled the log volume", 0.8));
        reflectReturns(structured(false, "INC-13 was about the refund module, which this change does not touch.",
                List.of()));

        assertThat(check(LOG + ": INFO -> DEBUG").get(0).status()).isEqualTo("NO_HISTORY");
    }

    @Test
    void aMemoryWarningNeedsAnIncidentThatBothRecallAndReflectPointTo() {
        recallReturns(RecallHit.of("INC-13", "Debug logging filled the log volume", 0.8));
        reflectReturns(structured(true, "Resembles INC-18.", List.of("INC-18")));

        assertThat(check(LOG + ": INFO -> DEBUG").get(0).status()).isEqualTo("NO_HISTORY");
    }

    @Test
    void inventedIncidentNumbersAreNeverShown() {
        recallReturns(RecallHit.of("INC-29", "Duplicate charges after a retry change", 0.8));
        reflectReturns(structured(true, "Resembles INC-99.", List.of("INC-99")));

        assertThat(check("payment.gateway.timeout-ms: 8000 -> 20000").get(0).status()).isEqualTo("NO_HISTORY");
    }

    @Test
    void aMemoryWarningKeepsOnlyRealCitations() {
        recallReturns(RecallHit.of("INC-29", "Duplicate charges with idempotency off", 0.8));
        reflectReturns(structured(true, "Turning idempotency off resembles INC-29.", List.of("INC-29", "INC-99")));

        KeyResult result = check("payment.retry.idempotency-enabled: true -> false").get(0);

        assertThat(result.status()).isEqualTo("WARNING");
        assertThat(result.warning().citedRefs()).containsExactly("INC-29");
        assertThat(result.warning().severity()).isEqualTo("SEV1");
    }

    // ------------------------------------------------------------------ previously judged safe

    @Test
    void theSameChangeJudgedSafeBeforeGetsANoteNotAWarning() {
        when(decisions.activeForKey(RETRY)).thenReturn(Optional.of(adr9));
        when(warnings.clearedFor(RETRY, "4")).thenReturn(Optional.of(new ClearedVerdict("WARN-42",
                Instant.parse("2026-06-16T10:33:00Z"), "payment.retry.idempotency-enabled is true.")));
        reflectReturns(structured(true, "WARN-42 flagged the same change and was a false positive.", List.of("WARN-42")));

        KeyResult result = guard.check(SERVICE, "v3.4", RETRY + ": 3 -> 4", false).results().get(0);

        assertThat(result.status()).isEqualTo("PREVIOUSLY_CLEARED");
        assertThat(result.warning()).isNull();
        assertThat(result.cleared().warningId()).isEqualTo("WARN-42");
        assertThat(result.cleared().date()).isEqualTo("2026-06-16");
        assertThat(result.cleared().decisionId()).isEqualTo("ADR-9");
        assertThat(result.cleared().citedRecords()).extracting(RecordSummary::id).contains("WARN-42", "ADR-9", "INC-29");
        verify(warnings, never()).insertWithNextId(any());
        verify(memory, never()).recall(any(RecallRequest.class));
    }

    @Test
    void aClearedChangeStillGetsItsNoteWhenMemoryIsDown() {
        when(decisions.activeForKey(RETRY)).thenReturn(Optional.of(adr9));
        when(warnings.clearedFor(RETRY, "4")).thenReturn(Optional.of(new ClearedVerdict("WARN-42",
                Instant.parse("2026-06-16T10:33:00Z"), "Idempotency is on.")));
        reflectFails();

        KeyResult result = check(RETRY + ": 3 -> 4").get(0);

        assertThat(result.status()).isEqualTo("PREVIOUSLY_CLEARED");
        assertThat(result.cleared().summary())
                .startsWith("Previously judged safe in WARN-42 (2026-06-16): Idempotency is on.")
                .contains("ADR-9 still applies");
    }

    // ------------------------------------------------------------------ parallel keys

    @Test
    void keysRunInParallelButWarningIdsFollowTheDiffOrder() {
        Deployment dep = new Deployment("DEP-2026-061", SERVICE, "v3.4", Instant.now());
        when(deployments.insert(eq(SERVICE), eq("v3.4"), anyList())).thenReturn(dep);
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        when(decisions.activeForKey(CRON)).thenReturn(Optional.of(adr11));
        // The first key's reflect is slow, so the second key finishes first.
        when(memory.reflect(contains(POOL), anyString(), any())).thenAnswer(invocation -> {
            Thread.sleep(300);
            return structured(true, "Resembles INC-18.", List.of("INC-18"));
        });
        when(memory.reflect(contains(CRON), anyString(), any()))
                .thenReturn(structured(true, "Resembles INC-33.", List.of("INC-33")));
        AtomicInteger next = new AtomicInteger(55);
        when(warnings.insertWithNextId(any(Warning.class))).thenAnswer(invocation -> {
            Warning w = invocation.getArgument(0);
            return new Warning("WARN-" + next.getAndIncrement(), w.deploymentId(), w.keyName(), w.createdAt(), w.kind(),
                    w.severity(), w.summary(), w.citedRefs(), w.failedAttempts(), w.priorFalsePositive(),
                    w.recommendation(), w.status(), null, null);
        });

        CheckResponse response = guard.check(SERVICE, "v3.4", POOL + ": 20 -> 60\n" + CRON + ": 0 30 1 * * * -> 0 30 23 * * *", false);

        assertThat(response.deploymentId()).isEqualTo("DEP-2026-061");
        assertThat(response.results()).extracting(KeyResult::key).containsExactly(POOL, CRON);
        assertThat(response.results()).extracting(r -> r.warning().id()).containsExactly("WARN-55", "WARN-56");
        verify(memorySync).retainDeploymentLater("DEP-2026-061");
    }

    @Test
    void aDryRunSavesNothing() {
        when(decisions.activeForKey(POOL)).thenReturn(Optional.of(adr7));
        reflectReturns(structured(true, "Resembles INC-18.", List.of("INC-18")));

        CheckResponse response = guard.check(SERVICE, "proposed", POOL + ": 20 -> 60", true);

        assertThat(response.deploymentId()).isNull();
        assertThat(response.results().get(0).warning().id()).isNull();
        verify(deployments, never()).insert(anyString(), anyString(), anyList());
        verify(warnings, never()).insertWithNextId(any());
        verify(memorySync, never()).retainDeploymentLater(anyString());
    }

    // ------------------------------------------------------------------ helpers

    private List<KeyResult> check(String diff) {
        return guard.check(SERVICE, "v3.4", diff, true).results();
    }

    private void recallReturns(RecallHit... hits) {
        when(memory.recall(any(RecallRequest.class))).thenReturn(new RecallResponse(List.of(hits), Map.of()));
    }

    private void reflectReturns(ReflectAnswer answer) {
        when(memory.reflect(anyString(), anyString(), any())).thenReturn(answer);
    }

    private void reflectFails() {
        when(memory.reflect(anyString(), anyString(), any())).thenThrow(new MemoryUnavailableException("down", 503, null));
    }

    private static ReflectAnswer structured(boolean matches, String summary, List<String> cited) {
        return structured(matches, summary, cited, List.of(), "");
    }

    private static ReflectAnswer structured(boolean matches, String summary, List<String> cited, List<String> failed,
                                            String priorFalsePositive) {
        return new ReflectAnswer(summary, Map.of("matches_history", matches, "summary", summary, "cited_ids", cited,
                "failed_attempts", failed, "prior_false_positive", priorFalsePositive, "recommendation", "Check it."),
                "", BasedOn.empty(), 1000);
    }

    private static Warning warn42() {
        return new Warning("WARN-42", "DEP-2026-041", RETRY, Instant.parse("2026-06-16T10:33:00Z"), "DECISION_GUARD",
                "SEV1", "Resembles INC-29.", List.of("ADR-9", "INC-29"), List.of(), null, null, "FALSE_POSITIVE",
                "Idempotency was on.", Instant.parse("2026-06-16T12:33:00Z"));
    }
}
