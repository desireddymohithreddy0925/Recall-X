package com.recallx.recallx.api;

import com.recallx.recallx.common.NotFoundException;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One record's summary, for the popover on a cited ID chip. */
@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordLookup lookup;

    public RecordController(RecordLookup lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/{id}")
    public RecordSummary record(@PathVariable String id) {
        return lookup.describe(id).orElseThrow(() -> new NotFoundException("No record " + id));
    }
}
