package com.recallx.recallx.guard;

import com.recallx.recallx.store.ConfigChange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a config diff: one change per line, in the form "key: old -> new". Deterministic; no LLM involved. */
public final class DiffParser {

    static final int MAX_CHANGES = 20;
    static final int MAX_LINE_LENGTH = 300;

    private static final Pattern LINE = Pattern.compile("^\\s*([\\w.\\-]+)\\s*:\\s*(.+?)\\s*->\\s*(.+?)\\s*$");

    private DiffParser() { }

    public static List<ConfigChange> parse(String diff) {
        if (diff == null || diff.isBlank()) {
            throw new DiffParseException("The diff is empty. Write one change per line: key: old -> new");
        }
        List<ConfigChange> changes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] lines = diff.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) continue;
            if (line.length() > MAX_LINE_LENGTH) {
                throw new DiffParseException("Line " + (i + 1) + " is longer than " + MAX_LINE_LENGTH + " characters");
            }
            Matcher m = LINE.matcher(line);
            if (!m.matches()) {
                throw new DiffParseException("Line " + (i + 1) + " is not in the form 'key: old -> new': " + line.trim());
            }
            if (!seen.add(m.group(1))) {
                throw new DiffParseException("Line " + (i + 1) + " changes " + m.group(1) + " again. List each key once.");
            }
            changes.add(new ConfigChange(m.group(1), m.group(2), m.group(3)));
            if (changes.size() > MAX_CHANGES) {
                throw new DiffParseException("Check at most " + MAX_CHANGES + " changes at a time");
            }
        }
        if (changes.isEmpty()) {
            throw new DiffParseException("The diff is empty. Write one change per line: key: old -> new");
        }
        return changes;
    }
}
