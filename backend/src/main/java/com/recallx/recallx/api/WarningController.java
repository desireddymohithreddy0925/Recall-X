package com.recallx.recallx.api;

import com.recallx.recallx.service.StatsService;
import com.recallx.recallx.service.VerdictService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    private final VerdictService verdicts;
    private final StatsService stats;

    public WarningController(VerdictService verdicts, StatsService stats) {
        this.verdicts = verdicts;
        this.stats = stats;
    }

    public record VerdictRequest(@NotBlank String verdict, @Size(max = 500) String reason) { }

    @GetMapping
    public List<StatsService.WarningSummary> list(@RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "50") int limit) {
        return stats.warnings(status, Math.min(Math.max(limit, 1), 200));
    }

    @PostMapping("/{id}/verdict")
    public VerdictService.VerdictResponse verdict(@PathVariable String id, @Valid @RequestBody VerdictRequest request) {
        return verdicts.record(id, request.verdict(), request.reason());
    }
}
