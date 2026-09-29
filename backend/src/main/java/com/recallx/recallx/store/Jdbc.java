package com.recallx.recallx.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Small JDBC helpers shared by the stores. */
final class Jdbc {

    private Jdbc() { }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static List<String> split(String value, String separator) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(separator)).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
