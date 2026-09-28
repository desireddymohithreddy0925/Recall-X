package com.recallx.recallx.controller;

import com.recallx.recallx.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(metricsService.computeMetrics(1L));
    }
}
