package com.recallx.recallx.integration;

import com.recallx.recallx.dto.integration.ExternalEngineeringContext;
import java.util.List;

public interface EngineeringContextIntegration {
    String getProviderName();
    List<ExternalEngineeringContext> fetchRecentActivity(Long organizationId);
    ExternalEngineeringContext fetchSpecificContext(String referenceId, Long organizationId);
}
