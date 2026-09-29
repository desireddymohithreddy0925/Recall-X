package com.recallx.recallx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Missing keys don't stop the app: the UI shows "Memory unavailable" or a model error instead. Say so at startup. */
@Component
public class StartupChecks {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final RecallxProperties props;

    public StartupChecks(RecallxProperties props) {
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnAboutMissingSettings() {
        if (props.hindsight().apiKey().isBlank()) {
            log.warn("HINDSIGHT_API_KEY is not set: every memory call will fail and the UI will show 'Memory unavailable'.");
        }
        if (props.llm().apiKey().isBlank()) {
            log.warn("LLM_API_KEY is not set: the memory-off answer on the Ask screen will show an error.");
        }
        if (props.adminToken().isBlank()) {
            log.warn("RECALLX_ADMIN_TOKEN is not set: /api/admin/** (sync, check, reset) will refuse every request.");
        }
        log.info("Hindsight bank '{}', retain path '{}', recall floor {}, reflect budgets ask={} check={}",
                props.hindsight().bankId(), props.hindsight().retainPath(), props.hindsight().minReranker(),
                props.hindsight().askBudget(), props.hindsight().checkBudget());
    }
}
