package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes everything created after the seed (rows with seeded = 0), so the demo can run again from the same state:
 * the next check gets DEP-2026-061 and WARN-55 again.
 */
@Repository
public class ResetStore {

    private final JdbcTemplate jdbc;

    public ResetStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Deletes live rows in foreign-key order and returns their IDs, so their memories can be removed too. */
    @Transactional
    public List<String> deleteUnseeded() {
        List<String> ids = new ArrayList<>();
        ids.addAll(jdbc.queryForList("SELECT id FROM warning WHERE seeded = 0", String.class));
        ids.addAll(jdbc.queryForList("SELECT id FROM incident WHERE seeded = 0", String.class));
        ids.addAll(jdbc.queryForList("SELECT id FROM architecture_decision WHERE seeded = 0", String.class));
        ids.addAll(jdbc.queryForList("SELECT id FROM deployment WHERE seeded = 0", String.class));

        jdbc.update("DELETE FROM warning WHERE seeded = 0 OR deployment_id IN (SELECT id FROM deployment WHERE seeded = 0)");
        jdbc.update("DELETE FROM architecture_decision WHERE seeded = 0");
        jdbc.update("DELETE FROM troubleshooting_attempt WHERE incident_id IN (SELECT id FROM incident WHERE seeded = 0)");
        jdbc.update("DELETE FROM incident_key WHERE incident_id IN (SELECT id FROM incident WHERE seeded = 0)");
        jdbc.update("DELETE FROM incident WHERE seeded = 0");
        jdbc.update("UPDATE incident SET caused_by = NULL WHERE caused_by IN (SELECT id FROM (SELECT id FROM deployment WHERE seeded = 0) d)");
        jdbc.update("DELETE FROM deployment_change WHERE deployment_id IN (SELECT id FROM deployment WHERE seeded = 0)");
        jdbc.update("DELETE FROM deployment WHERE seeded = 0");
        return ids;
    }
}
