package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RiskAnalysisResponse {
    private String status; // "MEMORY-BASED RISK DETECTED" or "NO HISTORICAL SIMILARITY"
    private String warningMessage; // "This change resembles historical changes associated with previous incidents."
    
    private String currentChange;
    
    private List<RiskEvidence> evidence;
    private Double overallConfidence;

    @Data
    @Builder
    public static class RiskEvidence {
        private String historicalExperience;
        private String similarity;
        private String previousIncident;
        private String previouslyFailed;
        private String previouslyWorked;
        private String lessonLearned;
        private String preventiveAction;
        private String referenceId;
    }
}
