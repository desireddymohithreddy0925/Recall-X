package com.recallx.recallx.integration;

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
