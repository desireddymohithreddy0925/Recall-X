package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.MetricsDTO;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    
    // In a real implementation, this queries count() across the JPA repositories.
    
    public MetricsDTO computeMetrics(Long organizationId) {
        return MetricsDTO.builder()
                .totalExperiences(124L)
                .totalIncidents(45L)
                .totalDeployments(78L)
                .totalArchitectureDecisions(12L)
                .totalFailedFixes(23L)
                .totalRecurringPatterns(4L)
                .totalLessons(34L)
                .build();
    }
}
