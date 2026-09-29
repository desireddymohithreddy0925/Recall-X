package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.RiskAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DeploymentAnalysisServiceTest {

    private DeploymentAnalysisService service;

    @BeforeEach
    void setUp() {
        DecisionGuardService guard = new DecisionGuardService();
        service = new DeploymentAnalysisService(guard);
    }

    @Test
    void testNoHistoricalSimilarity() {
        // Gated: No ACTIVE decision or incident matches "Update css colors"
        RiskAnalysisResponse response = service.analyzeDeployment("Update css colors", 1L);
        assertEquals("NO HISTORICAL SIMILARITY", response.getStatus());
        assertTrue(response.getEvidence().isEmpty());
    }

    @Test
    void testOneHistoricalSimilarity() {
        // Gated: "Update connection pool" matches the Decision Guard mock
        RiskAnalysisResponse response = service.analyzeDeployment("Update connection pool", 1L);
        assertEquals("MEMORY-BASED RISK DETECTED", response.getStatus());
        assertFalse(response.getEvidence().isEmpty());
    }

    @Test
    void testFailedFixHistory() {
        RiskAnalysisResponse response = service.analyzeDeployment("Update connection pool", 1L);
        assertNotNull(response.getEvidence().get(0).getPreviouslyFailed());
        assertTrue(response.getEvidence().get(0).getPreviouslyFailed().length() > 0);
    }
    
    @Test
    void testUnrelatedDeployment() {
        RiskAnalysisResponse response = service.analyzeDeployment("Update readme", 1L);
        assertEquals("NO HISTORICAL SIMILARITY", response.getStatus());
    }

    @Test
    void testOrganizationIsolation() {
        assertDoesNotThrow(() -> service.analyzeDeployment("Update connection pool", 2L));
    }
}
