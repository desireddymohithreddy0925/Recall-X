package com.recallx.recallx.api;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.memory.MemorySyncService;
import com.recallx.recallx.service.IncidentCaptureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentCaptureService capture;
    private final MemorySyncService memorySync;
    private final RecallxProperties props;

    public IncidentController(IncidentCaptureService capture, MemorySyncService memorySync, RecallxProperties props) {
        this.capture = capture;
        this.memorySync = memorySync;
        this.props = props;
    }

    public record IncidentCreated(String id, boolean retained) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentCreated create(@Valid @RequestBody IncidentCaptureService.IncidentRequest request) {
        String id = capture.capture(request, props.defaultService());   // committed first
        boolean retained = memorySync.retainIncident(id);                 // then sent to memory
        return new IncidentCreated(id, retained);
    }
}
