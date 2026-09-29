package com.recallx.recallx.api;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.guard.CheckResponse;
import com.recallx.recallx.guard.DecisionGuardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentCheckController {

    private final DecisionGuardService guard;
    private final RecallxProperties props;

    public DeploymentCheckController(DecisionGuardService guard, RecallxProperties props) {
        this.guard = guard;
        this.props = props;
    }

    /**
     * diff: one change per line, "key: old -> new" (DiffParser enforces 20 lines of 300 characters at most).
     * dryRun: check without saving or retaining anything (used by the MCP tool). Optional; missing means false.
     */
    public record CheckRequest(@Size(max = 64) String service, @Size(max = 32) String version,
                               @NotBlank @Size(max = 7000) String diff, Boolean dryRun) { }

    @PostMapping("/check")
    public CheckResponse check(@Valid @RequestBody CheckRequest request) {
        String service = request.service() == null || request.service().isBlank()
                ? props.defaultService() : request.service();
        String version = request.version() == null || request.version().isBlank() ? "unversioned" : request.version();
        return guard.check(service, version, request.diff(), Boolean.TRUE.equals(request.dryRun()));
    }
}
