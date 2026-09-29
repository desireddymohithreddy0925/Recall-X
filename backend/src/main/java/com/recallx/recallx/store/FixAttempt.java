package com.recallx.recallx.store;

/** outcome is FAILED, PARTIAL or RESOLVED. */
public record FixAttempt(int seq, String action, String outcome, String note) {
}
