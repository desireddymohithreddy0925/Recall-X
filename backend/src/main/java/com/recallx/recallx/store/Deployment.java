package com.recallx.recallx.store;

import java.time.Instant;

public record Deployment(String id, String serviceId, String version, Instant deployedAt) {
}
