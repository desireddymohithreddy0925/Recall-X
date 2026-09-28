package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.RiskAnalysisResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DeploymentAnalysisService {

    public RiskAnalysisResponse analyzeDeployment(String deploymentInfo, Long organizationId) {
        // Stub: Represents the workflow: Extract -> Retrieve -> Compare -> Pattern Analysis
        if (deploymentInfo == null || !deploymentInfo.contains("connection pool")) {
            return RiskAnalysisResponse.builder()
                    .status("NO HISTORICAL SIMILARITY")
                    .warningMessage("No significant historical precedents found for this change.")
                    .currentChange(deploymentInfo)
                    .evidence(List.of())
                    .overallConfidence(1.0)
                    .build();
        }

        return RiskAnalysisResponse.builder()
                .status("MEMORY-BASED RISK DETECTED")
                .warningMessage("This change resembles historical changes associated with previous incidents.")
                .currentChange(deploymentInfo)
                .overallConfidence(0.92)
                .evidence(List.of(
                    RiskAnalysisResponse.RiskEvidence.builder()
                            .historicalExperience("Connection pool change leading to database saturation")
                            .similarity("Both changes alter the maximum connection limit on the payment DB")
                            .previousIncident("INC-1024: Payment Service Outage")
                            .previouslyFailed("Increasing the timeout without capping the pool size")
                            .previouslyWorked("Reverting the pool size and implementing circuit breakers")
                            .lessonLearned("Uncapped connection pools overwhelm the primary writer DB instance")
                            .preventiveAction("Ensure circuit breaker is deployed alongside pool changes")
                            .referenceId("hindsight-id-99812")
                            .build()
                ))
                .build();
    }
}
