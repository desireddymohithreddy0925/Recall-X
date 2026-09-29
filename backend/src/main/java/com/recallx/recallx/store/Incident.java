package com.recallx.recallx.store;

import java.time.Instant;

public record Incident(String id, String serviceId, String severity, Instant startedAt, Instant resolvedAt,
                       String symptom, String rootCause, String lesson, String causedBy) {
}
