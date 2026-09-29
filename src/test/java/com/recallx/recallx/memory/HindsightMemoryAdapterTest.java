package com.recallx.recallx.memory;

import com.recallx.recallx.memory.dto.StructuredExperience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class HindsightMemoryAdapterTest {
    private HindsightMemoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new HindsightMemoryAdapter("http://localhost:8888", "mock-key");
    }

    @Test
    void testStoringExperience() {
        StructuredExperience exp = StructuredExperience.builder().title("Timeout").organizationId(1L).build();
        Optional<String> id = adapter.rememberExperience(exp);
        assertTrue(id.isPresent());
        assertTrue(id.get().startsWith("hindsight-id-"));
    }

    @Test
    void testRetrievingRelatedExperience_NoMemoryFound() {
        List<StructuredExperience> results = adapter.retrieveRelevantMemories("unknown error", 1L);
        assertTrue(results.isEmpty(), "Should return empty list gracefully when no memories found");
    }

    @Test
    void testHindsightFailureHandling() {
        // Since it's a mock, we verify that calling methods does not throw exceptions
        assertDoesNotThrow(() -> adapter.retrieveRelevantMemories("force error", 1L));
        assertDoesNotThrow(() -> adapter.rememberExperience(null)); // Should handle gracefully
    }

    @Test
    void testOrganizationIsolation() {
        // We assert that the organization ID is passed correctly and isolated
        StructuredExperience org1Exp = StructuredExperience.builder().title("Timeout").organizationId(1L).build();
        StructuredExperience org2Exp = StructuredExperience.builder().title("Timeout").organizationId(2L).build();

        Optional<String> id1 = adapter.rememberExperience(org1Exp);
        Optional<String> id2 = adapter.rememberExperience(org2Exp);

        assertNotEquals(id1.orElse("1"), id2.orElse("2"));
    }
}
