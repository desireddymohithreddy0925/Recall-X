package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricsDTO {
    private long totalExperiences;
    private long totalIncidents;
    private long totalDeployments;
    private long totalArchitectureDecisions;
    private long totalFailedFixes;
    private long totalRecurringPatterns;
    private long totalLessons;
}
