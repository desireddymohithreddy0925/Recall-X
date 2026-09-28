package com.recallx.recallx.memory;
import com.recallx.recallx.memory.dto.StructuredExperience;
import java.util.List;
import java.util.Optional;

public interface OrganizationalMemoryService {
    Optional<String> rememberExperience(StructuredExperience experience);
    List<StructuredExperience> retrieveRelevantMemories(String contextQuery, Long organizationId);
    boolean relateExperiences(String sourceHindsightId, String targetHindsightId);
    List<StructuredExperience> searchHistoricalExperiences(String query, Long organizationId);
}
