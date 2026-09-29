package com.recallx.recallx.memory.hindsight;

import java.util.List;

/** Shown under every memory answer: how many memories reflect used, and which mental models and directives. */
public record Sources(int memories, List<String> mentalModels, List<String> directives) {

    public static Sources none() {
        return new Sources(0, List.of(), List.of());
    }
}
