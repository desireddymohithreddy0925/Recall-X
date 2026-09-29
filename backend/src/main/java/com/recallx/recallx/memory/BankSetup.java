package com.recallx.recallx.memory;

import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shapes how the bank reasons: its mission, disposition, directives and one mental model. Safe to run more than
 * once. Each step is best effort; whatever fails here can be set by hand in the Hindsight Cloud UI (docs/DEMO.md).
 */
@Component
public class BankSetup {

    public static final String NAME = "RECALL-X - Acme Pay";
    public static final String MISSION = "I am the engineering memory for Acme Pay's payment platform. "
            + "I prioritise root causes, fixes that failed, and the reasons behind configuration decisions.";

    /** Untagged, so they apply to every reflect call. Name to content. */
    public static final Map<String, String> DIRECTIVES = directives();

    public static final String MENTAL_MODEL_ID = "payment-config-rules";
    public static final String MENTAL_MODEL_NAME = "payment-service configuration rules";
    public static final String MENTAL_MODEL_QUERY = "Which payment-service configuration values were set deliberately, "
            + "what incident led to each, and what failed before?";

    private static final Logger log = LoggerFactory.getLogger(BankSetup.class);

    private final HindsightClient memory;

    public BankSetup(HindsightClient memory) {
        this.memory = memory;
    }

    /** Returns one line per step, for the sync report. */
    public List<String> apply() {
        List<String> notes = new ArrayList<>();
        // Skepticism 4 keeps it from over-warning; literalism 4 keeps it exact about config values.
        notes.add(memory.configureBank(NAME, MISSION, 4, 4, 2)
                ? "mission and disposition set" : "mission and disposition NOT set: use the Cloud UI");
        try {
            Set<String> existing = memory.directiveNames();
            int created = 0;
            for (Map.Entry<String, String> d : DIRECTIVES.entrySet()) {
                if (!existing.contains(d.getKey())) {
                    memory.createDirective(d.getKey(), d.getValue());
                    created++;
                }
            }
            notes.add("directives: " + created + " created, " + (DIRECTIVES.size() - created) + " already present");
        } catch (MemoryUnavailableException e) {
            log.warn("[memory] directives not set: {}", e.getMessage());
            notes.add("directives NOT set: use the Cloud UI");
        }
        try {
            notes.add(memory.createMentalModel(MENTAL_MODEL_ID, MENTAL_MODEL_NAME, MENTAL_MODEL_QUERY)
                    ? "mental model created" : "mental model already present");
        } catch (MemoryUnavailableException e) {
            log.warn("[memory] mental model not created: {}", e.getMessage());
            notes.add("mental model NOT created: use the Cloud UI");
        }
        return notes;
    }

    private static Map<String, String> directives() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("Cite record IDs", "Cite the record ID (INC-, ADR-, DEP- or WARN-) for every claim.");
        d.put("Never invent history", "If memory holds no relevant record, say so. Never invent an incident, decision, ID or number.");
        d.put("Name failed fixes", "Mention any fix that was tried before and failed.");
        d.put("Mention false positives", "If an earlier warning on a similar change was marked a false positive, mention it and its reason.");
        d.put("Similarity, not prediction", "Describe similarity to past events. Never predict that a change will fail.");
        d.put("Newest decision wins", "If a decision has been superseded, use the newer one.");
        return Collections.unmodifiableMap(d);
    }
}
