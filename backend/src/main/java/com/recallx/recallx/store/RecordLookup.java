package com.recallx.recallx.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Cross-table checks. This is where every ID the model cites is verified against the database. */
@Repository
public class RecordLookup {

    private static final int TITLE_MAX = 90;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public RecordLookup(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    /** Returns the IDs that exist, in the order given, without duplicates. */
    public List<String> existingIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        Set<String> found = new HashSet<>(named.queryForList(
                "SELECT id FROM incident WHERE id IN (:ids) "
                        + "UNION SELECT id FROM architecture_decision WHERE id IN (:ids) "
                        + "UNION SELECT id FROM deployment WHERE id IN (:ids) "
                        + "UNION SELECT id FROM warning WHERE id IN (:ids)",
                params, String.class));
        return ids.stream().filter(found::contains).distinct().toList();
    }

    /** The worst severity among the given incidents ('SEV1' sorts before 'SEV2'), or null if there are none. */
    public String worstSeverity(Collection<String> ids) {
        List<String> incidentIds = ids.stream().filter(id -> id.startsWith("INC-")).toList();
        if (incidentIds.isEmpty()) return null;
        return named.queryForObject("SELECT MIN(severity) FROM incident WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", incidentIds), String.class);
    }

    public Optional<String> severityOf(String incidentId) {
        if (incidentId == null) return Optional.empty();
        return jdbc.queryForList("SELECT severity FROM incident WHERE id = ?", String.class, incidentId).stream().findFirst();
    }

    /** Summaries for ID chips, in the order given. IDs that don't exist are left out. */
    public List<RecordSummary> describe(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Map<String, RecordSummary> found = new HashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        named.query("SELECT id, started_at, severity, symptom FROM incident WHERE id IN (:ids)", params, rs -> {
            found.put(rs.getString("id"), new RecordSummary(rs.getString("id"), "INCIDENT",
                    day(Jdbc.instant(rs, "started_at")), rs.getString("severity"), title(rs.getString("symptom"))));
        });
        named.query("SELECT id, decided_at, decision FROM architecture_decision WHERE id IN (:ids)", params, rs -> {
            found.put(rs.getString("id"), new RecordSummary(rs.getString("id"), "DECISION",
                    day(Jdbc.instant(rs, "decided_at")), null, title(rs.getString("decision"))));
        });
        named.query("SELECT id, deployed_at, version FROM deployment WHERE id IN (:ids)", params, rs -> {
            found.put(rs.getString("id"), new RecordSummary(rs.getString("id"), "DEPLOYMENT",
                    day(Jdbc.instant(rs, "deployed_at")), null, "Deployment " + rs.getString("version")));
        });
        named.query("SELECT id, created_at, key_name, status FROM warning WHERE id IN (:ids)", params, rs -> {
            found.put(rs.getString("id"), new RecordSummary(rs.getString("id"), "WARNING",
                    day(Jdbc.instant(rs, "created_at")), null,
                    "Warning on " + rs.getString("key_name") + " (" + rs.getString("status").toLowerCase().replace('_', ' ') + ")"));
        });
        return ids.stream().distinct().map(found::get).filter(Objects::nonNull).toList();
    }

    public Optional<RecordSummary> describe(String id) {
        return describe(List.of(id)).stream().findFirst();
    }

    /** The canonical config key names, for the incident form's key picker. */
    public List<String> configKeys() {
        return jdbc.queryForList("SELECT key_name FROM config_key ORDER BY key_name", String.class);
    }

    public List<String> existingConfigKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return List.of();
        Set<String> found = new HashSet<>(named.queryForList("SELECT key_name FROM config_key WHERE key_name IN (:keys)",
                new MapSqlParameterSource("keys", keys), String.class));
        return keys.stream().filter(found::contains).distinct().toList();
    }

    public Counts counts() {
        return new Counts(count("incident"), count("architecture_decision"), count("deployment"), count("warning"));
    }

    private long count(String table) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    private static String day(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDate().toString();
    }

    /** The first sentence, cut to fit on a chip's tooltip. */
    private static String title(String text) {
        if (text == null) return "";
        String first = text.split("(?<=\\.)\\s", 2)[0].trim();
        return first.length() > TITLE_MAX ? first.substring(0, TITLE_MAX - 3).trim() + "..." : first;
    }
}
