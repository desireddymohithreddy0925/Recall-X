package com.recallx.recallx.controller;

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
