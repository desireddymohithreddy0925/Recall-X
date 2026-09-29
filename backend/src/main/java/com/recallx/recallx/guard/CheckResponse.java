package com.recallx.recallx.guard;

import java.util.List;

/** deploymentId is null for a dry run. */
public record CheckResponse(String deploymentId, List<KeyResult> results) {
}
