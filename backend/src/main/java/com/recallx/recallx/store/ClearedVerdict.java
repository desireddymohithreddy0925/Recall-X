package com.recallx.recallx.store;

import java.time.Instant;

/** An earlier warning on the same change (same key, same new value) that an engineer marked a false positive. */
public record ClearedVerdict(String warningId, Instant createdAt, String reason) {
}
