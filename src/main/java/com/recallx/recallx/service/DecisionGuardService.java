package com.recallx.recallx.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DecisionGuardService {
    
    // Simulating MySQL JPA Repository lookup for ACTIVE decisions on changed config keys.
    // In a real implementation, this runs a deterministic SQL query.
    
    public Optional<String> findActiveDecisionForConfigKey(String configKeyName) {
        if (configKeyName != null && configKeyName.contains("connection pool")) {
            // Stubbed lookup: Found an ACTIVE decision in MySQL
            return Optional.of("ADR-7");
        }
        return Optional.empty();
    }
    
    public List<String> findRelatedIncidents(String configKeyName) {
        if (configKeyName != null && configKeyName.contains("retry")) {
            // Stubbed lookup: Found a historical incident directly linked to this config key in MySQL
            return List.of("INC-29");
        }
        return List.of();
    }
}
