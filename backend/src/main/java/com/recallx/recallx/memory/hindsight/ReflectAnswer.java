package com.recallx.recallx.memory.hindsight;

import java.util.Map;

/**
 * The parts of a reflect response RECALL-X uses.
 *
 * @param text                  the markdown answer
 * @param structuredOutput      JSON matching response_schema, or null
 * @param structuredOutputError set when Hindsight could not produce the structured output
 * @param basedOn               the memories, mental models and directives reflect used
 * @param totalTokens           from usage, logged to track cost; null if not returned
 */
public record ReflectAnswer(String text, Map<String, Object> structuredOutput, String structuredOutputError,
                            BasedOn basedOn, Integer totalTokens) {

    public ReflectAnswer {
        text = text == null ? "" : text;
        structuredOutputError = structuredOutputError == null ? "" : structuredOutputError;
        basedOn = basedOn == null ? BasedOn.empty() : basedOn;
    }
}
