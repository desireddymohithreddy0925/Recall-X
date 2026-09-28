package com.recallx.recallx.controller;

import com.recallx.recallx.service.PatternDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patterns")
public class PatternController {

    private final PatternDetectionService patternService;

    public PatternController(PatternDetectionService patternService) {
        this.patternService = patternService;
    }

    @GetMapping
    public ResponseEntity<?> listPatterns() {
        return ResponseEntity.ok(patternService.detectPatterns(1L));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> viewPattern(@PathVariable Long id) {
        return patternService.getPattern(id, 1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/evidence")
    public ResponseEntity<?> getPatternEvidence(@PathVariable Long id) {
        return patternService.getPattern(id, 1L)
                .map(p -> ResponseEntity.ok(p.getEvidenceReferences()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/lessons")
    public ResponseEntity<?> getPatternLessons(@PathVariable Long id) {
        return patternService.getPattern(id, 1L)
                .map(p -> ResponseEntity.ok(p.getRelatedLessons()))
                .orElse(ResponseEntity.notFound().build());
    }
}
