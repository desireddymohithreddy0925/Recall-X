package com.recallx.recallx.store;

import java.time.Instant;
import java.util.List;

/** kind is DECISION_GUARD or HISTORY. status is PENDING, USEFUL, FALSE_POSITIVE or IGNORED. */
public record Warning(String id, String deploymentId, String keyName, Instant createdAt, String kind,
                      String severity, String summary, List<String> citedRefs, List<String> failedAttempts,
                      String priorFalsePositive, String recommendation, String status, String verdictReason,
                      Instant decidedAt) {
}
