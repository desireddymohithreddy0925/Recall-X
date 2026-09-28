package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ReasonedInsight {
    private String summary;
    private List<String> historicalExperiences;
    private List<String> similarities;
    private List<String> failedApproaches;
    private List<String> successfulApproaches;
    private List<String> lessons;
    private List<String> preventiveActions;
    private Double confidence;
    private List<String> evidence;
}
