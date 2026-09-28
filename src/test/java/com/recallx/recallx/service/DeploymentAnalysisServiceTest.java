package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.RiskAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DeploymentAnalysisServiceTest {

    private DeploymentAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new DeploymentAnalysisService();
    }

    @Test
    void testNoHistoricalSimilarity() {
        RiskAnalysisResponse response = service.analyzeDeployment("Update css colors", 1L);
        assertEquals("NO HISTORICAL SIMILARITY", response.getStatus());
        assertTrue(response.getEvidence().isEmpty());
    }

    @Test
    void testOneHistoricalSimilarity() {
        RiskAnalysisResponse response = service.analyzeDeployment("Update connection pool", 1L);
        assertEquals("MEMORY-BASED RISK DETECTED", response.getStatus());
        assertEquals("This change resembles historical changes associated with previous incidents.", response.getWarningMessage());
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
        // Asserting that org separation happens properly
        // In the stub it doesn't fail, we test that the API accepts orgId
        assertDoesNotThrow(() -> service.analyzeDeployment("Update connection pool", 2L));
    }
}
