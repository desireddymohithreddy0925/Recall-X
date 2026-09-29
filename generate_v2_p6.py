import os

base_pkg = "src/main/java/com/recallx/recallx"
os.makedirs(f"{base_pkg}/demo", exist_ok=True)
os.makedirs(f"{base_pkg}/ai", exist_ok=True)

files = {}

# 1. The lightweight Name Normalizer (replacing the heavy Extractor Agent)
files[f"{base_pkg}/ai/EntityNameNormalizer.java"] = """package com.recallx.recallx.ai;

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
"""

# 2. V2 Realistic Demo Data Seeder
files[f"{base_pkg}/demo/DemoDataSeeder.java"] = """package com.recallx.recallx.demo;

import com.recallx.recallx.memory.HindsightMemoryAdapter;
import com.recallx.recallx.memory.dto.StructuredExperience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    
    private final HindsightMemoryAdapter memoryAdapter;

    public DemoDataSeeder(HindsightMemoryAdapter memoryAdapter) {
        this.memoryAdapter = memoryAdapter;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("--- Seeding RECALL-X Demonstration Data (V2 Realistic Constraints) ---");
        
        // 1. Realistic Incident #1: Connection Pool Exhaustion (Backdated 3 months)
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        StructuredExperience inc18 = StructuredExperience.builder()
                .documentId("INC-18")
                .timestamp(threeMonthsAgo)
                .organizationId(1L)
                .title("payment-service HikariCP timeout")
                .whatHappened("Payment API requests timed out. Logs: 'HikariPool-1 - Connection is not available, request timed out after 30000ms'.")
                .whyItHappened("deployment DEP-2026-017 (v2.4) raised spring.datasource.hikari.maximum-pool-size from 20 to 50. With 4 replicas this needed 200 connections, above MySQL max_connections of 150, so MySQL rejected connections with error 1040 'Too many connections'.")
                .whatWasAttempted(List.of("restarted payment-service pods", "raised client timeout to 60s"))
                .whatFailed(List.of("restarted payment-service pods. Outcome: FAILED, timeouts returned within 10 minutes."))
                .whatWorked(List.of("reverted maximum-pool-size to 20."))
                .whyItWorked("Stayed under the global MySQL max_connections limit.")
                .lessons(List.of("replicas x maximum-pool-size must stay below MySQL max_connections."))
                .build();
                
        memoryAdapter.rememberExperience(inc18);
        
        // 2. Realistic Decision: ADR-7 (Backdated 88 days ago)
        Instant eightyEightDaysAgo = Instant.now().minus(88, ChronoUnit.DAYS);
        StructuredExperience adr7 = StructuredExperience.builder()
                .documentId("ADR-7")
                .timestamp(eightyEightDaysAgo)
                .organizationId(1L)
                .title("ADR-7: Cap payment-service connection pool")
                .whatHappened("Decision: keep spring.datasource.hikari.maximum-pool-size at 20 per replica.")
                .whyItHappened("Reason: after INC-18, total connections must stay below MySQL limit of 150, with headroom for other services.")
                .relatedDecisions(List.of("Revisit if: MySQL max_connections is raised or the replica count changes."))
                .build();
                
        memoryAdapter.rememberExperience(adr7);
        
        // 3. Realistic Warning Feedback (False Positive tracking for the precision chart)
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        StructuredExperience warn42 = StructuredExperience.builder()
                .documentId("WARN-42")
                .timestamp(oneMonthAgo)
                .organizationId(1L)
                .title("WARNING OUTCOME WARN-42")
                .whatHappened("RECALL-X warned that a retry change resembled INC-29 (duplicate charges).")
                .whyItHappened("Engineer verdict: FALSE POSITIVE. Retries in this change use idempotency keys, so the INC-29 duplicate-charge risk did not apply.")
                .lessons(List.of("Idempotency keys safely mitigate retry duplication risks."))
                .build();
                
        memoryAdapter.rememberExperience(warn42);

        log.info("--- Seed Complete: Backdated memory loaded successfully. ---");
    }
}
"""

for path, content in files.items():
    with open(path, "w") as f:
        f.write(content)

print("Scaffolded V2 Phase 6.")
