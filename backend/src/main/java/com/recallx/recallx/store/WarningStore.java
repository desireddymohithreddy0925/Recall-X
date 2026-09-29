package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class WarningStore {

    private static final RowMapper<Warning> WARNING = (rs, n) -> new Warning(
            rs.getString("id"), rs.getString("deployment_id"), rs.getString("key_name"),
            Jdbc.instant(rs, "created_at"), rs.getString("kind"), rs.getString("severity"),
            rs.getString("summary"), Jdbc.split(rs.getString("cited_refs"), ","),
            Jdbc.split(rs.getString("failed_attempts"), "\n"), rs.getString("prior_false_positive"),
            rs.getString("recommendation"), rs.getString("status"), rs.getString("verdict_reason"),
            Jdbc.instant(rs, "decided_at"));

    private final JdbcTemplate jdbc;

    public WarningStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Gives the warning the next WARN-n ID and inserts it. Synchronized, and deliberately not transactional, so the
     * insert is committed before the next caller reads MAX(id): parallel checks never get the same ID.
     */
    public synchronized Warning insertWithNextId(Warning w) {
        Integer next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 6) AS UNSIGNED)), 0) + 1 FROM warning", Integer.class);
        Warning saved = new Warning("WARN-" + next, w.deploymentId(), w.keyName(), w.createdAt(), w.kind(), w.severity(),
                w.summary(), w.citedRefs(), w.failedAttempts(), w.priorFalsePositive(), w.recommendation(), w.status(),
                w.verdictReason(), w.decidedAt());
        jdbc.update("INSERT INTO warning (id, deployment_id, key_name, created_at, kind, severity, summary, cited_refs, "
                        + "failed_attempts, prior_false_positive, recommendation, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                saved.id(), saved.deploymentId(), saved.keyName(), Jdbc.ts(saved.createdAt()), saved.kind(),
                saved.severity(), saved.summary(), String.join(",", saved.citedRefs()),
                String.join("\n", saved.failedAttempts()), saved.priorFalsePositive(), saved.recommendation(), saved.status());
        return saved;
    }

    public Optional<Warning> findById(String id) {
        return jdbc.query("SELECT * FROM warning WHERE id = ?", WARNING, id).stream().findFirst();
    }

    /**
     * The learning loop's deterministic half: if the newest rated warning on this exact change (same key, same new
     * value) was marked a false positive, return it. A later USEFUL verdict on the same change cancels it.
     */
    public Optional<ClearedVerdict> clearedFor(String keyName, String newValue) {
        return jdbc.query("SELECT w.id, w.created_at, w.status, w.verdict_reason FROM warning w "
                                + "JOIN deployment_change c ON c.deployment_id = w.deployment_id AND c.key_name = w.key_name "
                                + "WHERE w.key_name = ? AND c.new_value = ? AND w.status IN ('USEFUL', 'FALSE_POSITIVE') "
                                + "ORDER BY w.created_at DESC LIMIT 1",
                        (rs, n) -> "FALSE_POSITIVE".equals(rs.getString("status"))
                                ? new ClearedVerdict(rs.getString("id"), Jdbc.instant(rs, "created_at"), rs.getString("verdict_reason"))
                                : null,
                        keyName, newValue)
                .stream().filter(c -> c != null).findFirst();
    }

    /** Newest first. status may be null for all statuses. */
    public List<Warning> list(String status, int limit) {
        if (status == null || status.isBlank()) {
            return jdbc.query("SELECT * FROM warning ORDER BY created_at DESC LIMIT ?", WARNING, limit);
        }
        return jdbc.query("SELECT * FROM warning WHERE status = ? ORDER BY created_at DESC LIMIT ?", WARNING, status, limit);
    }

    /** Records a verdict only if the warning is still PENDING. Returns the number of rows changed (0 or 1). */
    public int recordVerdict(String id, String status, String reason, Instant at) {
        return jdbc.update("UPDATE warning SET status = ?, verdict_reason = ?, decided_at = ? WHERE id = ? AND status = 'PENDING'",
                status, reason, Jdbc.ts(at), id);
    }

    /** Warnings with a verdict that is not yet in memory. */
    public List<Warning> pendingRetain() {
        return jdbc.query("SELECT * FROM warning WHERE status <> 'PENDING' AND retained_at IS NULL ORDER BY created_at", WARNING);
    }

    public void markRetained(String id) {
        jdbc.update("UPDATE warning SET retained_at = ? WHERE id = ?", Jdbc.ts(Instant.now()), id);
    }

    /** Useful / (useful + false positive) per month. Ignored and pending warnings are excluded. */
    public List<MonthPrecision> precisionByMonth() {
        return jdbc.query("SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, "
                        + "SUM(CASE WHEN status = 'USEFUL' THEN 1 ELSE 0 END) AS useful, "
                        + "SUM(CASE WHEN status = 'FALSE_POSITIVE' THEN 1 ELSE 0 END) AS fp "
                        + "FROM warning GROUP BY ym ORDER BY ym",
                (rs, n) -> {
                    int useful = rs.getInt("useful");
                    int fp = rs.getInt("fp");
                    Integer pct = (useful + fp) == 0 ? null : Math.round(100f * useful / (useful + fp));
                    return new MonthPrecision(rs.getString("ym"), useful, fp, pct);
                });
    }
}
