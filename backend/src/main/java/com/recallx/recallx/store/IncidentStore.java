package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class IncidentStore {

    private static final RowMapper<Incident> INCIDENT = (rs, n) -> new Incident(
            rs.getString("id"), rs.getString("service_id"), rs.getString("severity"),
            Jdbc.instant(rs, "started_at"), Jdbc.instant(rs, "resolved_at"),
            rs.getString("symptom"), rs.getString("root_cause"), rs.getString("lesson"), rs.getString("caused_by"));

    private static final RowMapper<FixAttempt> ATTEMPT = (rs, n) -> new FixAttempt(
            rs.getInt("seq"), rs.getString("action"), rs.getString("outcome"), rs.getString("note"));

    private final JdbcTemplate jdbc;

    public IncidentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Incident> findById(String id) {
        return jdbc.query("SELECT * FROM incident WHERE id = ?", INCIDENT, id).stream().findFirst();
    }

    public List<FixAttempt> attempts(String incidentId) {
        return jdbc.query("SELECT * FROM troubleshooting_attempt WHERE incident_id = ? ORDER BY seq", ATTEMPT, incidentId);
    }

    public List<String> keys(String incidentId) {
        return jdbc.queryForList("SELECT key_name FROM incident_key WHERE incident_id = ? ORDER BY key_name",
                String.class, incidentId);
    }

    public List<Incident> pendingRetain() {
        return jdbc.query("SELECT * FROM incident WHERE retained_at IS NULL ORDER BY started_at", INCIDENT);
    }

    public void markRetained(String id) {
        jdbc.update("UPDATE incident SET retained_at = ? WHERE id = ?", Jdbc.ts(Instant.now()), id);
    }

    public String nextId() {
        Integer next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 5) AS UNSIGNED)), 0) + 1 FROM incident", Integer.class);
        return "INC-" + next;
    }

    public void insert(Incident incident, List<FixAttempt> attempts, List<String> keys) {
        jdbc.update("INSERT INTO incident (id, service_id, severity, started_at, resolved_at, symptom, root_cause, lesson, caused_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                incident.id(), incident.serviceId(), incident.severity(), Jdbc.ts(incident.startedAt()),
                Jdbc.ts(incident.resolvedAt()), incident.symptom(), incident.rootCause(), incident.lesson(),
                incident.causedBy());
        for (FixAttempt a : attempts) {
            jdbc.update("INSERT INTO troubleshooting_attempt (incident_id, seq, action, outcome, note) VALUES (?, ?, ?, ?, ?)",
                    incident.id(), a.seq(), a.action(), a.outcome(), a.note());
        }
        for (String key : keys) {
            jdbc.update("INSERT INTO incident_key (incident_id, key_name) VALUES (?, ?)", incident.id(), key);
        }
    }
}
