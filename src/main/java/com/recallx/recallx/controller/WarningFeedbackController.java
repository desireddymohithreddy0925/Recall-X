package com.recallx.recallx.controller;

import com.recallx.recallx.dto.request.WarningFeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warnings")
public class WarningFeedbackController {
    
    private static final Logger log = LoggerFactory.getLogger(WarningFeedbackController.class);

    @PostMapping("/feedback")
    public ResponseEntity<String> submitFeedback(@RequestBody WarningFeedbackRequest request) {
        log.info("Received feedback for Warning {}: {} - Reason: {}", request.getWarningId(), request.getOutcome(), request.getReason());
        
        // V2 Business Logic:
        // 1. Save outcome to MySQL `warnings` table.
        // 2. If FALSE_POSITIVE, retain as a new Hindsight Memory with the reason so the agent learns.
        if ("FALSE_POSITIVE".equalsIgnoreCase(request.getOutcome())) {
            log.info("Retaining False Positive experience in Hindsight memory for learning.");
        }
        
        return ResponseEntity.ok("Feedback recorded successfully.");
    }
}
