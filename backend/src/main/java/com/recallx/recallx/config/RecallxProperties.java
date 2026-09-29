package com.recallx.recallx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** All RECALL-X settings, bound from the recallx.* block in application.yml (which reads the environment and .env). */
@ConfigurationProperties("recallx")
public record RecallxProperties(
        @DefaultValue("payment-service") String defaultService,
        boolean seedMemoryOnStartup,
        @DefaultValue("") String adminToken,
        @DefaultValue("http://localhost:5173") List<String> corsAllowedOrigins,
        @DefaultValue Hindsight hindsight,
        @DefaultValue Llm llm) {

    public record Hindsight(
            @DefaultValue("https://api.hindsight.vectorize.io") String baseUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue("recallx-acme") String bankId,
            @DefaultValue("/memories") String retainPath,
            @DefaultValue("0.3") double minReranker,
            @DefaultValue("mid") String askBudget,
            @DefaultValue("high") String checkBudget,
            @DefaultValue("10s") Duration connectTimeout,
            @DefaultValue("60s") Duration readTimeout) {
    }

    public record Llm(
            @DefaultValue("https://api.groq.com/openai/v1") String baseUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue("openai/gpt-oss-120b") String model,
            @DefaultValue("qwen/qwen3.8-27b") String fallbackModel) {
    }
}
