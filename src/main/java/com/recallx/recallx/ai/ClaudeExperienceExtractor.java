package com.recallx.recallx.ai;

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
