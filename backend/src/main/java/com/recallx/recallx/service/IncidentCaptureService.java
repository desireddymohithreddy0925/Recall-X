package com.recallx.recallx.service;

import com.recallx.recallx.common.BadRequestException;
import com.recallx.recallx.store.FixAttempt;
import com.recallx.recallx.store.Incident;
import com.recallx.recallx.store.IncidentStore;
import com.recallx.recallx.store.RecordLookup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** "What we already learned": records a closed incident, including the fixes that failed. */
@Service
public class IncidentCaptureService {

    private static final Set<String> SEVERITIES = Set.of("SEV1", "SEV2", "SEV3");
    private static final Set<String> OUTCOMES = Set.of("FAILED", "PARTIAL", "RESOLVED");

    private final IncidentStore incidents;
    private final RecordLookup lookup;

    public IncidentCaptureService(IncidentStore incidents, RecordLookup lookup) {
        this.incidents = incidents;
        this.lookup = lookup;
    }

    public record AttemptRequest(@NotBlank @Size(max = 500) String action, @NotBlank String outcome,
                                 @Size(max = 500) String note) { }

    public record IncidentRequest(@Size(max = 64) String service, @NotBlank String severity, @NotBlank String startedAt,
                                  @NotBlank String resolvedAt, @NotBlank @Size(max = 2000) String symptom,
                                  @Size(max = 2000) String rootCause,
                                  @NotEmpty @Size(max = 10) List<@Valid AttemptRequest> attempts,
                                  @Size(max = 2000) String lesson, Boolean nothingNewLearned, List<String> configKeys,
                                  @Size(max = 32) String causedBy) { }

    @Transactional
    public String capture(IncidentRequest r, String defaultService) {
        if (!SEVERITIES.contains(r.severity())) throw new BadRequestException("severity must be SEV1, SEV2 or SEV3");
        Instant started = parse(r.startedAt(), "startedAt");
        Instant resolved = parse(r.resolvedAt(), "resolvedAt");
        if (!resolved.isAfter(started)) throw new BadRequestException("resolvedAt must be after startedAt");

        List<FixAttempt> attempts = new ArrayList<>();
        for (int i = 0; i < r.attempts().size(); i++) {
            AttemptRequest a = r.attempts().get(i);
            if (!OUTCOMES.contains(a.outcome())) {
                throw new BadRequestException("Attempt " + (i + 1) + ": outcome must be FAILED, PARTIAL or RESOLVED");
            }
            attempts.add(new FixAttempt(i + 1, a.action().trim(), a.outcome(), a.note()));
        }
        long resolvedCount = attempts.stream().filter(a -> "RESOLVED".equals(a.outcome())).count();
        if (resolvedCount != 1 || !"RESOLVED".equals(attempts.get(attempts.size() - 1).outcome())) {
            throw new BadRequestException("Exactly one attempt must be RESOLVED, and it must be the last one");
        }
        boolean hasLesson = r.lesson() != null && !r.lesson().isBlank();
        if (!hasLesson && !Boolean.TRUE.equals(r.nothingNewLearned())) {
            throw new BadRequestException("Add a lesson, or tick \"Nothing new learned\"");
        }

        List<String> keys = r.configKeys() == null ? List.of() : r.configKeys();
        List<String> known = lookup.existingConfigKeys(keys);
        if (known.size() != keys.stream().distinct().count()) {
            throw new BadRequestException("Unknown config key(s). Use the canonical names from the config key list");
        }
        String causedBy = r.causedBy() == null || r.causedBy().isBlank() ? null : r.causedBy().trim();
        if (causedBy != null && lookup.existingIds(List.of(causedBy)).isEmpty()) {
            throw new BadRequestException("No deployment " + causedBy);
        }

        String id = incidents.nextId();
        String service = r.service() == null || r.service().isBlank() ? defaultService : r.service();
        incidents.insert(new Incident(id, service, r.severity(), started, resolved, r.symptom().trim(),
                blankToNull(r.rootCause()), hasLesson ? r.lesson().trim() : "Nothing new learned.", causedBy), attempts, known);
        return id;
    }

    private static Instant parse(String value, String field) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be an ISO-8601 UTC time, e.g. 2026-10-03T10:15:00Z");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
