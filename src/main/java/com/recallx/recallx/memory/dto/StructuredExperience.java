package com.recallx.recallx.memory.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StructuredExperience {
    private String title;
    private String whatHappened;
    private String whyItHappened;
    private List<String> whatWasAttempted;
    private List<String> whatFailed;
    private List<String> whatWorked;
    private String whyItWorked;
    private List<String> relatedServices;
    private List<String> relatedDeployments;
    private List<String> relatedDecisions;
    private List<String> lessons;
    private List<String> preventiveActions;
    private Long organizationId;
}
