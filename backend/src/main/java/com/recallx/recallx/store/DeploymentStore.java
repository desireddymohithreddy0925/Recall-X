package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class DeploymentStore {

    private static final RowMapper<Deployment> DEPLOYMENT = (rs, n) -> new Deployment(
            rs.getString("id"), rs.getString("service_id"), rs.getString("version"), Jdbc.instant(rs, "deployed_at"));

    private static final RowMapper<ConfigChange> CHANGE = (rs, n) -> new ConfigChange(
            rs.getString("key_name"), rs.getString("old_value"), rs.getString("new_value"));

    private final JdbcTemplate jdbc;

    public DeploymentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Gives the deployment the next DEP-yyyy-nnn ID and inserts it with its changes. Synchronized and not
     * transactional, so the row is committed before the next caller reads MAX(id). DiffParser rejects duplicate
     * keys, so a change insert can't fail on the primary key.
     */
    public synchronized Deployment insert(String serviceId, String version, List<ConfigChange> changes) {
        Integer next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 10) AS UNSIGNED)), 0) + 1 FROM deployment", Integer.class);
        String id = String.format("DEP-%d-%03d", Year.now(ZoneOffset.UTC).getValue(), next);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO deployment (id, service_id, version, deployed_at) VALUES (?, ?, ?, ?)",
                id, serviceId, version, Jdbc.ts(now));
        for (ConfigChange c : changes) {
            jdbc.update("INSERT INTO deployment_change (deployment_id, key_name, old_value, new_value) VALUES (?, ?, ?, ?)",
                    id, c.keyName(), c.oldValue(), c.newValue());
        }
        return new Deployment(id, serviceId, version, now);
    }

    public Optional<Deployment> findById(String id) {
        return jdbc.query("SELECT * FROM deployment WHERE id = ?", DEPLOYMENT, id).stream().findFirst();
    }

    public List<ConfigChange> changes(String deploymentId) {
        return jdbc.query("SELECT * FROM deployment_change WHERE deployment_id = ? ORDER BY key_name", CHANGE, deploymentId);
    }

    /** Deployments not yet in memory that change at least one tracked key. Routine deployments stay out of memory. */
    public List<Deployment> trackedPendingRetain() {
        return jdbc.query("SELECT d.* FROM deployment d WHERE d.retained_at IS NULL AND EXISTS ("
                + "SELECT 1 FROM deployment_change c JOIN config_key k ON k.key_name = c.key_name "
                + "WHERE c.deployment_id = d.id) ORDER BY d.deployed_at", DEPLOYMENT);
    }

    public void markRetained(String id) {
        jdbc.update("UPDATE deployment SET retained_at = ? WHERE id = ?", Jdbc.ts(Instant.now()), id);
    }
}
