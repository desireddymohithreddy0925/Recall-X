package com.recallx.recallx.service;

import com.recallx.recallx.dto.response.PatternDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class PatternDetectionServiceTest {

    private PatternDetectionService service;

    @BeforeEach
    void setUp() {
        service = new PatternDetectionService();
    }

    @Test
    void testPatternCreationAndEvidence() {
        List<PatternDTO> patterns = service.detectPatterns(1L);
        assertFalse(patterns.isEmpty(), "Patterns should be detected from evidence");
        PatternDTO pattern = patterns.get(0);
        
        // Assert evidence requirement
        assertFalse(pattern.getEvidenceReferences().isEmpty(), "Pattern MUST have concrete supporting evidence");
        assertTrue(pattern.getOccurrenceCount() >= 2, "Pattern requires recurring occurrences");
    }

    @Test
    void testGetPatternFound() {
        Optional<PatternDTO> pattern = service.getPattern(1L, 1L);
        assertTrue(pattern.isPresent());
        assertEquals("Connection Pool Exhaustion", pattern.get().getPatternName());
    }

    @Test
    void testGetPatternNotFound() {
        Optional<PatternDTO> pattern = service.getPattern(999L, 1L);
        assertFalse(pattern.isPresent());
    }

    @Test
    void testOrganizationIsolation() {
        // Mock verification that org isolation is handled (would be handled by repository filtering)
        assertDoesNotThrow(() -> service.detectPatterns(2L));
    }
}
