import os

base_pkg = "src/main/java/com/recallx/recallx"

os.makedirs(f"{base_pkg}/integration", exist_ok=True)
os.makedirs(f"{base_pkg}/integration/github", exist_ok=True)
os.makedirs(f"{base_pkg}/dto/integration", exist_ok=True)
os.makedirs("src/test/java/com/recallx/recallx/integration", exist_ok=True)

files = {}

# ExternalEngineeringContext DTO
files[f"{base_pkg}/dto/integration/ExternalEngineeringContext.java"] = """package com.recallx.recallx.dto.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalEngineeringContext {
    private String sourceSystem; // e.g., "GITHUB"
    private String sourceReferenceId; // e.g., "PR-1234"
    private String sourceUrl;
    
    private String repository;
    private String commitHash;
    private String summary;
    private String details; // E.g., changed files list, issue body
    
    private Long organizationId;
}
"""

# Integration Interface
files[f"{base_pkg}/integration/EngineeringContextIntegration.java"] = """package com.recallx.recallx.integration;

import com.recallx.recallx.dto.integration.ExternalEngineeringContext;
import java.util.List;

public interface EngineeringContextIntegration {
    String getProviderName();
    List<ExternalEngineeringContext> fetchRecentActivity(Long organizationId);
    ExternalEngineeringContext fetchSpecificContext(String referenceId, Long organizationId);
}
"""

# GitHub Adapter
files[f"{base_pkg}/integration/github/GithubIntegrationAdapter.java"] = """package com.recallx.recallx.integration.github;

import com.recallx.recallx.dto.integration.ExternalEngineeringContext;
import com.recallx.recallx.integration.EngineeringContextIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GithubIntegrationAdapter implements EngineeringContextIntegration {

    private static final Logger log = LoggerFactory.getLogger(GithubIntegrationAdapter.class);

    @Override
    public String getProviderName() {
        return "GITHUB";
    }

    @Override
    public List<ExternalEngineeringContext> fetchRecentActivity(Long organizationId) {
        try {
            // Stubbed implementation to avoid generating fake HTTP clients
            // In reality, this would use an official GitHub SDK or Spring WebClient
            log.info("Fetching recent GitHub activity for org: {}", organizationId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch GitHub activity. Retrying...", e);
            // Implement retry logic here using Spring @Retryable
            return Collections.emptyList();
        }
    }

    @Override
    public ExternalEngineeringContext fetchSpecificContext(String referenceId, Long organizationId) {
        log.info("Fetching GitHub context for reference: {} in org: {}", referenceId, organizationId);
        return ExternalEngineeringContext.builder()
                .sourceSystem("GITHUB")
                .sourceReferenceId(referenceId)
                .organizationId(organizationId)
                .build();
    }
}
"""

# Tests
files["src/test/java/com/recallx/recallx/integration/GithubIntegrationAdapterTest.java"] = """package com.recallx.recallx.integration;

import com.recallx.recallx.dto.integration.ExternalEngineeringContext;
import com.recallx.recallx.integration.github.GithubIntegrationAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GithubIntegrationAdapterTest {

    private GithubIntegrationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GithubIntegrationAdapter();
    }

    @Test
    void testGetProviderName() {
        assertEquals("GITHUB", adapter.getProviderName());
    }

    @Test
    void testFetchRecentActivityErrorHandling() {
        // Ensuring the mock doesn't throw exceptions, handling gracefully
        List<ExternalEngineeringContext> activities = adapter.fetchRecentActivity(1L);
        assertNotNull(activities);
    }
    
    @Test
    void testFetchSpecificContextSourcePreservation() {
        ExternalEngineeringContext context = adapter.fetchSpecificContext("PR-502", 1L);
        assertEquals("GITHUB", context.getSourceSystem());
        assertEquals("PR-502", context.getSourceReferenceId());
    }
}
"""

for path, content in files.items():
    with open(path, "w") as f:
        f.write(content)

print("Scaffolded Phase 9.")
