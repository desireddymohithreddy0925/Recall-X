package com.recallx.recallx.service;

import com.recallx.recallx.TestProperties;
import com.recallx.recallx.llm.BaselineLlmClient;
import com.recallx.recallx.memory.hindsight.BasedOn;
import com.recallx.recallx.memory.hindsight.HindsightClient;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import com.recallx.recallx.memory.hindsight.ReflectAnswer;
import com.recallx.recallx.store.RecordLookup;
import com.recallx.recallx.store.RecordSummary;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AskServiceTest {

    private final BaselineLlmClient llm = mock(BaselineLlmClient.class);
    private final HindsightClient memory = mock(HindsightClient.class);
    private final RecordLookup lookup = mock(RecordLookup.class);
    private final AskService ask = new AskService(llm, memory, lookup, TestProperties.props());

    @Test
    void whenMemoryIsDownTheMemoryPanelSaysSoAndNeverShowsTheOtherAnswer() {
        when(llm.answer("Timeouts?")).thenReturn(new BaselineLlmClient.LlmAnswer("Restart the service.", "openai/gpt-oss-120b"));
        when(memory.reflect(anyString(), eq("mid"), isNull())).thenThrow(new MemoryUnavailableException("down", 503, null));

        AskService.AskResponse response = ask.ask("Timeouts?");

        assertThat(response.withoutMemory().text()).isEqualTo("Restart the service.");
        assertThat(response.withoutMemory().model()).isEqualTo("openai/gpt-oss-120b");
        assertThat(response.withMemory().text()).isNull();
        assertThat(response.withMemory().error()).isEqualTo("Memory unavailable");
    }

    @Test
    void onlyRecordsThatExistBecomeCitationChips() {
        when(llm.answer(anyString())).thenReturn(new BaselineLlmClient.LlmAnswer("Generic advice.", "openai/gpt-oss-120b"));
        when(memory.reflect(anyString(), eq("mid"), isNull())).thenReturn(new ReflectAnswer(
                "- INC-18: restarting failed. INC-99 also mentioned.", null, "", BasedOn.empty(), 900));
        when(lookup.describe(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter("INC-18"::equals)
                    .map(id -> new RecordSummary(id, "INCIDENT", "2026-03-12", "SEV2", "Payment API requests timed out.")).toList();
        });

        AskService.AskResponse response = ask.ask("Timeouts?");

        assertThat(response.withMemory().citedRecords()).extracting(RecordSummary::id).containsExactly("INC-18");
        assertThat(response.withMemory().model()).isEqualTo(AskService.MEMORY_MODEL_LABEL);
        assertThat(response.withoutMemory().citedRecords()).isEmpty();
    }

    @Test
    void aModelFailureOnlyAffectsItsOwnPanel() {
        when(llm.answer(anyString())).thenThrow(new IllegalStateException("LLM_API_KEY is not set"));
        when(memory.reflect(anyString(), eq("mid"), isNull())).thenReturn(new ReflectAnswer("- INC-18", null, "",
                BasedOn.empty(), null));
        when(lookup.describe(anyCollection())).thenReturn(List.of());

        AskService.AskResponse response = ask.ask("Timeouts?");

        assertThat(response.withoutMemory().error()).contains("LLM_API_KEY");
        assertThat(response.withMemory().text()).isEqualTo("- INC-18");
    }
}
