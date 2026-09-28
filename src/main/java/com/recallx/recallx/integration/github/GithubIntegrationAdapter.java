package com.recallx.recallx.integration.github;

import com.recallx.recallx.dto.integration.ExternalEngineeringContext;
import com.recallx.recallx.integration.EngineeringContextIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GithubIntegrationAdapter implements EngineeringContextIntegration {

    private static final Logger log = LoggerFactory.getLogger(GithubIntegrationAdapter.class);

    @Override
    public String getProviderName() {
        return "GITHUB";
    }

    @Override
    public List<ExternalEngineeringContext> fetchRecentActivity(Long organizationId) {
        try {
            // Stubbed implementation to avoid generating fake HTTP clients
            // In reality, this would use an official GitHub SDK or Spring WebClient
            log.info("Fetching recent GitHub activity for org: {}", organizationId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch GitHub activity. Retrying...", e);
            // Implement retry logic here using Spring @Retryable
            return Collections.emptyList();
        }
    }

    @Override
    public ExternalEngineeringContext fetchSpecificContext(String referenceId, Long organizationId) {
        log.info("Fetching GitHub context for reference: {} in org: {}", referenceId, organizationId);
        return ExternalEngineeringContext.builder()
                .sourceSystem("GITHUB")
                .sourceReferenceId(referenceId)
                .organizationId(organizationId)
                .build();
    }
}
