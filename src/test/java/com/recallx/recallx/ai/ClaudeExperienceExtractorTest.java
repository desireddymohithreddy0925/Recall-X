package com.recallx.recallx.ai;

import com.recallx.recallx.dto.response.ExtractedExperience;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClaudeExperienceExtractorTest {

    private final ExperienceExtractor extractor = new ClaudeExperienceExtractor();

    @Test
    void testValidExtraction() {
        String input = "Database timed out. We restarted the service but it failed. Finally we increased connection pool.";
        ExtractedExperience experience = extractor.extractFromRawInput(input);
        
        assertNotNull(experience);
        assertEquals("INCIDENT", experience.getEventType());
        assertTrue(experience.getConfidence() > 0.9);
    }
    
    @Test
    void testIncompleteInputDoesNotHallucinate() {
        String input = "Something broke.";
        ExtractedExperience experience = extractor.extractFromRawInput(input);
        
        assertNotNull(experience);
        // Expecting nulls instead of fabricated data for missing fields
        assertNull(experience.getRootCause());
        assertNull(experience.getSuccessfulApproach());
    }
}
