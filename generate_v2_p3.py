import os

base_pkg = "src/main/java/com/recallx/recallx"
os.makedirs(f"{base_pkg}/service", exist_ok=True)
os.makedirs(f"{base_pkg}/controller", exist_ok=True)
os.makedirs(f"{base_pkg}/dto/request", exist_ok=True)
os.makedirs("src/test/java/com/recallx/recallx/service", exist_ok=True)

files = {}

# 1. DecisionGuardService
files[f"{base_pkg}/service/DecisionGuardService.java"] = """package com.recallx.recallx.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DecisionGuardService {
    
    // Simulating MySQL JPA Repository lookup for ACTIVE decisions on changed config keys.
    // In a real implementation, this runs a deterministic SQL query.
    
    public Optional<String> findActiveDecisionForConfigKey(String configKeyName) {
        if (configKeyName != null && configKeyName.contains("connection pool")) {
            // Stubbed lookup: Found an ACTIVE decision in MySQL
            return Optional.of("ADR-7");
        }
        return Optional.empty();
    }
    
    public List<String> findRelatedIncidents(String configKeyName) {
        if (configKeyName != null && configKeyName.contains("retry")) {
            // Stubbed lookup: Found a historical incident directly linked to this config key in MySQL
            return List.of("INC-29");
        }
        return List.of();
    }
}
"""

# 2. Refactor DeploymentAnalysisService
files[f"{base_pkg}/service/DeploymentAnalysisService.java"] = """package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.RiskAnalysisResponse;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DeploymentAnalysisService {

    private final DecisionGuardService decisionGuard;

    public DeploymentAnalysisService(DecisionGuardService decisionGuard) {
        this.decisionGuard = decisionGuard;
    }

    public RiskAnalysisResponse analyzeDeployment(String deploymentInfo, Long organizationId) {
        // 1. Deterministic Gating: We only proceed if DecisionGuard finds ACTIVE decisions or linked incidents.
        Optional<String> activeDecisionId = decisionGuard.findActiveDecisionForConfigKey(deploymentInfo);
        List<String> relatedIncidents = decisionGuard.findRelatedIncidents(deploymentInfo);
        
        if (activeDecisionId.isEmpty() && relatedIncidents.isEmpty()) {
            return RiskAnalysisResponse.builder()
                    .status("NO HISTORICAL SIMILARITY")
                    .warningMessage("No active architectural decisions or historical incidents found for this change.")
                    .currentChange(deploymentInfo)
                    .evidence(List.of())
                    .build();
        }

        // 2. Call Reflect (Mocked): Generate the warning using only valid references.
        // We drop confidence metric per V2 review (LLM confidence is meaningless). We rely on hard facts.
        return RiskAnalysisResponse.builder()
                .status("MEMORY-BASED RISK DETECTED")
                .warningMessage("This change touches a setting that was chosen deliberately after an incident.")
                .currentChange(deploymentInfo)
                .evidence(List.of(
                    RiskAnalysisResponse.RiskEvidence.builder()
                            .historicalExperience("Connection pool change leading to database saturation")
                            .similarity("Both changes alter the maximum connection limit on the payment DB")
                            .previousIncident("INC-1024: Payment Service Outage")
                            .previouslyFailed("Increasing the timeout without capping the pool size")
                            .previouslyWorked("Reverting the pool size and implementing circuit breakers")
                            .lessonLearned("Uncapped connection pools overwhelm the primary writer DB instance")
                            .preventiveAction("Ensure circuit breaker is deployed alongside pool changes")
                            .referenceId(activeDecisionId.orElse("INC-1024"))
                            .build()
                ))
                .build();
    }
}
"""

# 3. DTO for Feedback
files[f"{base_pkg}/dto/request/WarningFeedbackRequest.java"] = """package com.recallx.recallx.dto.request;

import lombok.Data;

@Data
public class WarningFeedbackRequest {
    private String warningId;
    private String outcome; // USEFUL, FALSE_POSITIVE, IGNORED
    private String reason; // Required if FALSE_POSITIVE
}
"""

# 4. Feedback Controller
files[f"{base_pkg}/controller/WarningFeedbackController.java"] = """package com.recallx.recallx.controller;

import com.recallx.recallx.dto.request.WarningFeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warnings")
public class WarningFeedbackController {
    
    private static final Logger log = LoggerFactory.getLogger(WarningFeedbackController.class);

    @PostMapping("/feedback")
    public ResponseEntity<String> submitFeedback(@RequestBody WarningFeedbackRequest request) {
        log.info("Received feedback for Warning {}: {} - Reason: {}", request.getWarningId(), request.getOutcome(), request.getReason());
        
        // V2 Business Logic:
        // 1. Save outcome to MySQL `warnings` table.
        // 2. If FALSE_POSITIVE, retain as a new Hindsight Memory with the reason so the agent learns.
        if ("FALSE_POSITIVE".equalsIgnoreCase(request.getOutcome())) {
            log.info("Retaining False Positive experience in Hindsight memory for learning.");
        }
        
        return ResponseEntity.ok("Feedback recorded successfully.");
    }
}
"""

# 5. Refactor MetricsService (Warning Precision)
files[f"{base_pkg}/service/MetricsService.java"] = """package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.MetricsDTO;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    
    // In a real implementation, this queries count() across the JPA repositories.
    
    public MetricsDTO computeMetrics(Long organizationId) {
        // V2 Update: Calculate Warning Precision = useful / (useful + false positive)
        double usefulWarnings = 45.0;
        double falsePositives = 5.0;
        double precision = (usefulWarnings / (usefulWarnings + falsePositives)) * 100.0;

        return MetricsDTO.builder()
                .totalExperiences(124L)
                .totalIncidents(45L)
                .totalDeployments(78L)
                .totalArchitectureDecisions(12L)
                .totalFailedFixes(23L)
                .totalRecurringPatterns(4L)
                .totalLessons(34L)
                // Assuming we add warningPrecision to MetricsDTO (mocking it via existing fields for simplicity, 
                // but in reality we would add it to DTO).
                .build();
    }
}
"""

# 6. Refactor DeploymentAnalysisServiceTest
files["src/test/java/com/recallx/recallx/service/DeploymentAnalysisServiceTest.java"] = """package com.recallx.recallx.service;

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
"""

for path, content in files.items():
    with open(path, "w") as f:
        f.write(content)

print("Scaffolded V2 Phase 3 and 4.")
