package com.recallx.recallx.service;

import com.recallx.recallx.config.RecallxProperties;
import com.recallx.recallx.llm.BaselineLlmClient;
import com.recallx.recallx.llm.Prompts;
import com.recallx.recallx.memory.RecordIds;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.ReflectAnswer;
import com.recallx.recallx.memory.hindsight.Sources;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The before/after: the same question answered by the model alone and by Hindsight reflect over the team's memory.
 * Each panel names the model that produced it (see {@link Prompts} for what the two prompts share).
 */
@Service
public class AskService {

    public static final String MEMORY_MODEL_LABEL = "Hindsight reflect";

    private final BaselineLlmClient llm;
    private final HindsightClient memory;
    private final RecordLookup lookup;
    private final RecallxProperties.Hindsight settings;

    public AskService(BaselineLlmClient llm, HindsightClient memory, RecordLookup lookup, RecallxProperties props) {
        this.llm = llm;
        this.memory = memory;
        this.lookup = lookup;
        this.settings = props.hindsight();
    }

    /** citedRecords and sources are empty on the memory-off side. text is null when error is set. */
    public record Panel(String model, String text, List<RecordSummary> citedRecords, Sources sources, String error) { }

    public record AskResponse(Panel withoutMemory, Panel withMemory) { }

    public AskResponse ask(String question) {
        CompletableFuture<BaselineLlmClient.LlmAnswer> without = CompletableFuture.supplyAsync(() -> llm.answer(question));
        CompletableFuture<ReflectAnswer> with = CompletableFuture.supplyAsync(
                () -> memory.reflect(Prompts.memoryQuery(question), settings.askBudget(), null));

        Panel withoutPanel;
        try {
            BaselineLlmClient.LlmAnswer answer = without.join();
            withoutPanel = new Panel(answer.model(), answer.text(), List.of(), Sources.none(), null);
        } catch (CompletionException e) {
            withoutPanel = new Panel(null, null, List.of(), Sources.none(), "The model is unavailable: " + rootMessage(e));
        }

        Panel withPanel;
        try {
            ReflectAnswer answer = with.join();
            // Only IDs that exist in MySQL become chips; an invented ID is never shown as a citation.
            List<RecordSummary> cited = lookup.describe(RecordIds.extract(answer.text()));
            withPanel = new Panel(MEMORY_MODEL_LABEL, answer.text(), cited, answer.basedOn().sources(), null);
        } catch (CompletionException e) {
            // Never fall back to the memory-free answer here: that would fake the comparison.
            withPanel = new Panel(MEMORY_MODEL_LABEL, null, List.of(), Sources.none(), "Memory unavailable");
        }
        return new AskResponse(withoutPanel, withPanel);
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }
}
