package com.recallx.recallx.api;

import com.recallx.recallx.store.RecordLookup;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The canonical config keys. The incident form picks from these, so every record uses the same names. */
@RestController
@RequestMapping("/api/config-keys")
public class ConfigKeyController {

    private final RecordLookup lookup;

    public ConfigKeyController(RecordLookup lookup) {
        this.lookup = lookup;
    }

    @GetMapping
    public List<String> keys() {
        return lookup.configKeys();
    }
}
