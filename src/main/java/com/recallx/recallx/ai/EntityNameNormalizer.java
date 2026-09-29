package com.recallx.recallx.ai;

import org.springframework.stereotype.Service;

@Service
public class EntityNameNormalizer {
    
    /**
     * V2 Refactor: Instead of a heavy LLM pipeline trying to deduce root causes,
     * this lightweight normalizer just maps free-text inputs (like "payment db" or "db pool")
     * to canonical MySQL entity names (like "payment-service" and "spring.datasource.hikari.maximum-pool-size")
     * so that the Hindsight entity graph links correctly.
     */
    public String normalizeConfigKey(String rawInput) {
        if (rawInput == null) return null;
        String lower = rawInput.toLowerCase();
        
        if (lower.contains("pool") || lower.contains("connection")) {
            return "spring.datasource.hikari.maximum-pool-size";
        } else if (lower.contains("retry") || lower.contains("duplicate")) {
            return "resilience4j.retry.payment-service.max-attempts";
        } else if (lower.contains("timeout") || lower.contains("latency")) {
            return "spring.cloud.gateway.httpclient.response-timeout";
        }
        
        return rawInput;
    }
    
    public String normalizeServiceName(String rawInput) {
        if (rawInput == null) return null;
        String lower = rawInput.toLowerCase();
        if (lower.contains("pay")) return "payment-service";
        if (lower.contains("auth")) return "auth-service";
        return "unknown-service";
    }
}
