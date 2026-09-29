package com.recallx.recallx.api;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.service.PatternService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patterns")
public class PatternsController {

    private final PatternService patterns;
    private final RecallxProperties props;

    public PatternsController(PatternService patterns, RecallxProperties props) {
        this.patterns = patterns;
        this.props = props;
    }

    @GetMapping
    public PatternService.PatternsResponse patterns(@RequestParam(required = false) String service) {
        return patterns.patterns(service == null || service.isBlank() ? props.defaultService() : service);
    }
}
