package com.recallx.recallx.service;

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
