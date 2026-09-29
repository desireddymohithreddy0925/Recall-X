package com.recallx.recallx.memory;

import com.recallx.recallx.store.ConfigChange;
import com.recallx.recallx.store.Decision;
import com.recallx.recallx.store.Deployment;
import com.recallx.recallx.store.FixAttempt;
import com.recallx.recallx.store.Incident;
import com.recallx.recallx.store.Warning;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders each record as short, explicit prose for retain. Causes and outcomes are written out plainly,
 * because Hindsight's entity graph links things mentioned together; it does not store "caused by" edges.
 * Every record carries its own ID in the text so recalled facts can be traced back to a database row.
 */
public final class MemoryTemplates {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private MemoryTemplates() { }

    public static String day(Instant instant) {
        return instant == null ? "unknown" : DAY.format(instant);
    }

    public static String incident(Incident i, List<FixAttempt> attempts, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        sb.append("[INCIDENT ").append(i.id()).append("] service=").append(i.serviceId())
                .append(" date=").append(day(i.startedAt())).append(" severity=").append(i.severity()).append('\n');
        sb.append("Symptom: ").append(i.symptom()).append('\n');
        if (notBlank(i.rootCause())) sb.append("Root cause: ").append(i.rootCause()).append('\n');
        if (!keys.isEmpty()) sb.append("Config keys involved: ").append(String.join(", ", keys)).append('\n');
        for (FixAttempt a : attempts) {
            sb.append("Attempt ").append(a.seq()).append(": ").append(a.action()).append(". Outcome: ").append(a.outcome());
            if (notBlank(a.note())) sb.append(". ").append(a.note());
            sb.append('\n');
        }
        if (notBlank(i.lesson())) sb.append("Lesson: ").append(i.lesson()).append('\n');
        return sb.toString().trim();
    }

    public static String decision(Decision d) {
        StringBuilder sb = new StringBuilder();
        sb.append("[DECISION ").append(d.id()).append("] service=").append(d.serviceId())
                .append(" date=").append(day(d.decidedAt())).append(" status=").append(d.status())
                .append(" key=").append(d.keyName()).append('\n');
        sb.append("Decision: ").append(d.decision()).append('\n');
        sb.append("Reason: ").append(d.reason());
        if (notBlank(d.linkedIncidentId())) sb.append('\n').append("Linked incident: ").append(d.linkedIncidentId());
        return sb.toString();
    }

    public static String deployment(Deployment d, List<ConfigChange> changes) {
        String list = changes.stream()
                .map(c -> c.keyName() + " " + c.oldValue() + " -> " + c.newValue())
                .collect(Collectors.joining("; "));
        return "[DEPLOYMENT " + d.id() + "] service=" + d.serviceId() + " version=" + d.version()
                + " date=" + day(d.deployedAt()) + "\nChanges: " + list + ".";
    }

    public static String warningOutcome(Warning w) {
        String verdict = switch (w.status()) {
            case "USEFUL" -> "USEFUL. The warning was right";
            case "FALSE_POSITIVE" -> "FALSE POSITIVE. The warning did not apply";
            case "IGNORED" -> "IGNORED";
            default -> w.status();
        };
        StringBuilder sb = new StringBuilder();
        sb.append("[WARNING OUTCOME ").append(w.id()).append("] deployment=").append(w.deploymentId())
                .append(" date=").append(day(w.createdAt())).append(" key=").append(w.keyName()).append('\n');
        sb.append("RECALL-X warned: ").append(w.summary()).append('\n');
        sb.append("Engineer verdict: ").append(verdict);
        if (notBlank(w.verdictReason())) sb.append(": ").append(w.verdictReason().trim());
        char last = sb.charAt(sb.length() - 1);
        if (last != '.' && last != '!' && last != '?') sb.append('.');
        return sb.toString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
