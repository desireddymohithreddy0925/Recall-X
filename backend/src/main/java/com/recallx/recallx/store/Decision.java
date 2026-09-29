package com.recallx.recallx.store;

import java.time.Instant;

/** An architecture decision record (ADR) that governs one config key. status is ACTIVE or SUPERSEDED. */
public record Decision(String id, String serviceId, String keyName, Instant decidedAt, String decision,
                       String reason, String status, String linkedIncidentId) {
}
