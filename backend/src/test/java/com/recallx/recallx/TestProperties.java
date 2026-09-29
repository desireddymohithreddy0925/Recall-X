package com.recallx.recallx;

import com.recallx.recallx.config.RecallxProperties;

import java.time.Duration;
import java.util.List;

/** Settings for unit tests: a fake Hindsight at https://hindsight.test and a fake model API at https://llm.test. */
public final class TestProperties {

    public static final double FLOOR = 0.42;

    private TestProperties() { }

    public static RecallxProperties props() {
        return props("hs-key", "llm-key");
    }

    public static RecallxProperties props(String hindsightKey, String llmKey) {
        return new RecallxProperties("payment-service", false, "admin-secret", List.of("http://localhost:5173"),
                new RecallxProperties.Hindsight("https://hindsight.test", hindsightKey, "test-bank", "/memories", FLOOR,
                        "mid", "high", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new RecallxProperties.Llm("https://llm.test/v1", llmKey, "openai/gpt-oss-120b", "qwen/qwen3.8-27b"));
    }
}
