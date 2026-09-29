package com.recallx.recallx.memory.hindsight;

import java.util.ArrayList;
import java.util.List;

/**
 * What reflect used to write its answer: memories (facts and observations), mental models and directives.
 * Only returned when the request includes "include": {"facts": {}}.
 */
public record BasedOn(List<Memory> memories, List<Ref> mentalModels, List<Ref> directives) {

    public record Memory(String id, String text, String type) { }

    /** A mental model or directive. name may be null for mental models. */
    public record Ref(String id, String name, String text) {
        String label() {
            return name != null && !name.isBlank() ? name : id;
        }
    }

    public BasedOn {
        memories = memories == null ? List.of() : List.copyOf(memories);
        mentalModels = mentalModels == null ? List.of() : List.copyOf(mentalModels);
        directives = directives == null ? List.of() : List.copyOf(directives);
    }

    public static BasedOn empty() {
        return new BasedOn(List.of(), List.of(), List.of());
    }

    /** The text of every memory and mental model used, scanned for record IDs. */
    public String text() {
        List<String> parts = new ArrayList<>();
        memories.forEach(m -> parts.add(m.text()));
        mentalModels.forEach(m -> parts.add(m.text()));
        return String.join("\n", parts.stream().filter(p -> p != null && !p.isBlank()).toList());
    }

    /** The "Sources" line under a memory answer. */
    public Sources sources() {
        return new Sources(memories.size(), mentalModels.stream().map(Ref::label).toList(),
                directives.stream().map(Ref::label).toList());
    }
}
