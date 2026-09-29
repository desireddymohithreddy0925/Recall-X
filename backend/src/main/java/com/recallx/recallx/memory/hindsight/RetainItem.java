package com.recallx.recallx.memory.hindsight;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One item for POST /v1/default/banks/{bank}/memories.
 *
 * @param content    the record rendered as text (see MemoryTemplates)
 * @param context    the record type, e.g. "incident"
 * @param timestamp  when the event happened (not when it was loaded)
 * @param documentId the record's own ID, so re-retaining replaces instead of duplicating
 * @param metadata   {type, ref, service}
 */
public record RetainItem(String content, String context, Instant timestamp,
                         String documentId, Map<String, String> metadata) {

    Map<String, Object> toJson() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("content", content);
        if (context != null) item.put("context", context);
        if (timestamp != null) item.put("timestamp", timestamp.toString());
        if (documentId != null) item.put("document_id", documentId);
        if (metadata != null && !metadata.isEmpty()) item.put("metadata", metadata);
        return item;
    }
}
