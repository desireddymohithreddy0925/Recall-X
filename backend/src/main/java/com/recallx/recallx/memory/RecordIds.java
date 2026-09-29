package com.recallx.recallx.memory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds record IDs (INC-18, ADR-7, DEP-2026-017, WARN-42) in text returned by Hindsight. */
public final class RecordIds {

    private static final String ID_PATTERN = "INC-\\d+|ADR-\\d+|DEP-\\d{4}-\\d{3}|WARN-\\d+";
    private static final Pattern ID = Pattern.compile("\\b(" + ID_PATTERN + ")\\b");
    private static final Pattern WHOLE = Pattern.compile(ID_PATTERN);

    private RecordIds() { }

    public static Set<String> extract(String text) {
        Set<String> ids = new LinkedHashSet<>();
        if (text == null) return ids;
        Matcher m = ID.matcher(text);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    /** True if the whole string is one record ID, e.g. a document_id returned by recall. */
    public static boolean isRecordId(String value) {
        return value != null && WHOLE.matcher(value).matches();
    }

    /** Incidents, decisions and warning outcomes count as history. A past deployment alone does not. */
    public static boolean isEvidence(String id) {
        return id.startsWith("INC-") || id.startsWith("ADR-") || id.startsWith("WARN-");
    }

    public static boolean isIncident(String id) {
        return id.startsWith("INC-");
    }
}
