package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExtractedExperience {
    private String eventType;
    private String summary;
    private String affectedService;
    private String symptoms;
    private String rootCause;
    private List<String> attemptedFixes;
    private List<String> failedApproaches;
    private String successfulApproach;
    private String relatedDeployment;
    private String architectureDecision;
    private String lessonLearned;
    private String preventiveAction;
    private Double confidence;
}
