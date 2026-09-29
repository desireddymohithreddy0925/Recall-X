package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DecisionStore {

    private static final RowMapper<Decision> DECISION = (rs, n) -> new Decision(
            rs.getString("id"), rs.getString("service_id"), rs.getString("key_name"),
            Jdbc.instant(rs, "decided_at"), rs.getString("decision"), rs.getString("reason"),
            rs.getString("status"), rs.getString("linked_incident_id"));

    private final JdbcTemplate jdbc;

    public DecisionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The newest ACTIVE decision governing this key. This lookup is what makes Decision Guard deterministic. */
    public Optional<Decision> activeForKey(String keyName) {
        return jdbc.query("SELECT * FROM architecture_decision WHERE key_name = ? AND status = 'ACTIVE' "
                + "ORDER BY decided_at DESC LIMIT 1", DECISION, keyName).stream().findFirst();
    }

    public Optional<Decision> findById(String id) {
        return jdbc.query("SELECT * FROM architecture_decision WHERE id = ?", DECISION, id).stream().findFirst();
    }

    public List<Decision> pendingRetain() {
        return jdbc.query("SELECT * FROM architecture_decision WHERE retained_at IS NULL ORDER BY decided_at", DECISION);
    }

    public void markRetained(String id) {
        jdbc.update("UPDATE architecture_decision SET retained_at = ? WHERE id = ?", Jdbc.ts(Instant.now()), id);
    }
}
