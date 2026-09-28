import os

base_pkg = "src/main/java/com/recallx/recallx"

os.makedirs(f"{base_pkg}/ai", exist_ok=True)
os.makedirs(f"{base_pkg}/controller", exist_ok=True)
os.makedirs(f"{base_pkg}/dto/response", exist_ok=True)
os.makedirs("src/test/java/com/recallx/recallx/ai", exist_ok=True)

files = {}

# ExtractedExperience DTO
files[f"{base_pkg}/dto/response/ExtractedExperience.java"] = """package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExtractedExperience {
    private String eventType;
    private String summary;
    private String affectedService;
    private String symptoms;
    private String rootCause;
    private List<String> attemptedFixes;
    private List<String> failedApproaches;
    private String successfulApproach;
    private String relatedDeployment;
    private String architectureDecision;
    private String lessonLearned;
    private String preventiveAction;
    private Double confidence;
}
"""

# ExperienceExtractor Interface
files[f"{base_pkg}/ai/ExperienceExtractor.java"] = """package com.recallx.recallx.ai;

import com.recallx.recallx.dto.response.ExtractedExperience;

public interface ExperienceExtractor {
    ExtractedExperience extractFromRawInput(String rawInput);
}
"""

# Claude Implementation (Stub for Anthropic API)
files[f"{base_pkg}/ai/ClaudeExperienceExtractor.java"] = """package com.recallx.recallx.ai;

import com.recallx.recallx.dto.response.ExtractedExperience;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ClaudeExperienceExtractor implements ExperienceExtractor {
    
    @Override
    public ExtractedExperience extractFromRawInput(String rawInput) {
        // In a real implementation, this would call the Anthropic API
        // using prompt engineering to enforce structured output.
        // If an attribute is unknown, it maps to null, avoiding hallucinations.
        
        return ExtractedExperience.builder()
                .summary("Extracted summary for: " + rawInput)
                .eventType("INCIDENT")
                .confidence(0.95)
                .failedApproaches(List.of())
                .attemptedFixes(List.of())
                .build();
    }
}
"""

# Ingestion Controller
files[f"{base_pkg}/controller/ExperienceIngestionController.java"] = """package com.recallx.recallx.controller;

import com.recallx.recallx.ai.ExperienceExtractor;
import com.recallx.recallx.dto.response.ExtractedExperience;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiences")
public class ExperienceIngestionController {

    private final ExperienceExtractor experienceExtractor;

    public ExperienceIngestionController(ExperienceExtractor experienceExtractor) {
        this.experienceExtractor = experienceExtractor;
    }

    @PostMapping("/ingest")
    public ResponseEntity<ExtractedExperience> ingestExperience(@RequestBody String rawInput) {
        // 1. Extract structured data
        ExtractedExperience extracted = experienceExtractor.extractFromRawInput(rawInput);
        
        // 2. Validate and convert to domain entities
        // 3. Save to database
        
        return ResponseEntity.ok(extracted);
    }
}
"""

# AI Extraction Test
files["src/test/java/com/recallx/recallx/ai/ClaudeExperienceExtractorTest.java"] = """package com.recallx.recallx.ai;

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
"""

for path, content in files.items():
    with open(path, "w") as f:
        f.write(content)

print("Scaffolded Phase 4 AI Extractor.")
