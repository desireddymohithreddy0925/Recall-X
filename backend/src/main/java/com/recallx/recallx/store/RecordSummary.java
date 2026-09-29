package com.recallx.recallx.store;

/**
 * A cited record as the UI shows it: an ID chip with its date. Always read from MySQL, never from the model.
 *
 * @param type     INCIDENT, DECISION, DEPLOYMENT or WARNING
 * @param date     yyyy-MM-dd (UTC)
 * @param severity SEV1-SEV3 for incidents, otherwise null
 */
public record RecordSummary(String id, String type, String date, String severity, String title) {
}
