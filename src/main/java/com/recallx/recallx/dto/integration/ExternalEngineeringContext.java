package com.recallx.recallx.dto.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalEngineeringContext {
    private String sourceSystem; // e.g., "GITHUB"
    private String sourceReferenceId; // e.g., "PR-1234"
    private String sourceUrl;
    
    private String repository;
    private String commitHash;
    private String summary;
    private String details; // E.g., changed files list, issue body
    
    private Long organizationId;
}
