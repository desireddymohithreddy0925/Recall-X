package com.recallx.recallx.memory.hindsight;

import com.recallx.recallx.TestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HindsightClientTest {

    private static final String BANK = "https://hindsight.test/v1/default/banks/test-bank";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final HindsightClient client = new HindsightClient(TestProperties.props(), builder);

    @Test
    void recallSendsTheScoreFloorAndReadsDocumentIdsAndScores() {
        server.expect(requestTo(BANK + "/memories/recall"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer hs-key"))
                .andExpect(jsonPath("$.query").value("payment-service: change x from 1 to 2"))
                .andExpect(jsonPath("$.types[0]").value("world"))
                .andExpect(jsonPath("$.min_scores.reranker").value(0.42))
                .andExpect(jsonPath("$.include").doesNotExist())
                .andRespond(withSuccess("""
                        {"results": [
                          {"id": "f1", "text": "Debug logging filled the disk", "type": "world",
                           "document_id": "INC-13", "occurred_start": "2026-03-07T14:10:00Z",
                           "scores": {"final": 0.7, "reranker": 0.81}},
                          {"id": "f2", "text": "No document id, but INC-18 is named here", "type": "world"}
                        ]}""", MediaType.APPLICATION_JSON));

        RecallResponse response = client.recall(RecallRequest.of("payment-service: change x from 1 to 2",
                List.of("world", "experience"), "mid", 2048).withMinReranker(0.42));

        assertThat(response.hits()).hasSize(2);
        RecallHit first = response.hits().get(0);
        assertThat(first.documentId()).isEqualTo("INC-13");
        assertThat(first.reranker()).isEqualTo(0.81);
        assertThat(first.recordIds()).containsExactly("INC-13");
        assertThat(response.hits().get(1).recordIds()).containsExactly("INC-18");   // falls back to the text
        server.verify();
    }

    @Test
    void recallForObservationsAsksForTheirSourceFacts() {
        server.expect(requestTo(BANK + "/memories/recall"))
                .andExpect(jsonPath("$.types[0]").value("observation"))
                .andExpect(jsonPath("$.include.source_facts").exists())
                .andRespond(withSuccess("""
                        {"results": [{"id": "o1", "text": "Pool-size changes keep causing connection errors.",
                                      "type": "observation", "source_fact_ids": ["f1", "f2"]}],
                         "source_facts": {"f1": {"id": "f1", "text": "a", "type": "world", "document_id": "INC-18"},
                                          "f2": {"id": "f2", "text": "b", "type": "world", "document_id": "INC-31"}}}""",
                        MediaType.APPLICATION_JSON));

        RecallResponse response = client.recall(RecallRequest.of("patterns", List.of("observation"), "low", 1024)
                .withSourceFacts());

        assertThat(response.hits().get(0).sourceFactIds()).containsExactly("f1", "f2");
        assertThat(response.sourceFacts().get("f2").documentId()).isEqualTo("INC-31");
    }

    @Test
    void reflectAsksForItsSourcesAndParsesThem() {
        server.expect(requestTo(BANK + "/reflect"))
                .andExpect(jsonPath("$.budget").value("high"))
                .andExpect(jsonPath("$.include.facts").exists())
                .andExpect(jsonPath("$.response_schema.type").value("object"))
                .andRespond(withSuccess("""
                        {"text": "Resembles INC-18.",
                         "structured_output": {"matches_history": true, "summary": "Resembles INC-18."},
                         "based_on": {
                           "memories": [{"id": "m1", "text": "[INCIDENT INC-18] pool raised", "type": "world"},
                                        {"id": "m2", "text": "restart failed", "type": "experience"}],
                           "mental_models": [{"id": "payment-config-rules", "text": "ADR-7 keeps the pool at 20"}],
                           "directives": [{"id": "d1", "name": "Cite record IDs", "content": "Cite the record ID"}]},
                         "usage": {"input_tokens": 900, "output_tokens": 100, "total_tokens": 1000}}""",
                        MediaType.APPLICATION_JSON));

        ReflectAnswer answer = client.reflect("q", "high", Map.of("type", "object", "properties", Map.of()));

        assertThat(answer.structuredOutput()).containsEntry("matches_history", true);
        assertThat(answer.totalTokens()).isEqualTo(1000);
        assertThat(answer.basedOn().sources().memories()).isEqualTo(2);
        assertThat(answer.basedOn().sources().mentalModels()).containsExactly("payment-config-rules");
        assertThat(answer.basedOn().sources().directives()).containsExactly("Cite record IDs");
        assertThat(answer.basedOn().text()).contains("INC-18", "ADR-7");
    }

    @Test
    void retainSendsEachRecordWithItsIdDateAndMetadata() {
        server.expect(requestTo(BANK + "/memories"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.async").value(true))
                .andExpect(jsonPath("$.items[0].document_id").value("INC-18"))
                .andExpect(jsonPath("$.items[0].timestamp").value("2026-03-12T09:40:00Z"))
                .andExpect(jsonPath("$.items[0].context").value("incident"))
                .andExpect(jsonPath("$.items[0].metadata.ref").value("INC-18"))
                .andRespond(withSuccess("{\"operation_id\": \"op-1\"}", MediaType.APPLICATION_JSON));

        String operation = client.retain(List.of(new RetainItem("[INCIDENT INC-18] ...", "incident",
                Instant.parse("2026-03-12T09:40:00Z"), "INC-18", Map.of("type", "incident", "ref", "INC-18"))), true);

        assertThat(operation).isEqualTo("op-1");
    }

    @Test
    void outOfCreditsBecomesMemoryUnavailableWithAClearReason() {
        server.expect(requestTo(BANK + "/reflect")).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));

        assertThatThrownBy(() -> client.reflect("q", "mid", null))
                .isInstanceOf(MemoryUnavailableException.class)
                .hasMessageContaining("402")
                .hasMessageContaining("credits")
                .satisfies(e -> assertThat(((MemoryUnavailableException) e).status()).isEqualTo(402));
    }

    @Test
    void aServerErrorBecomesMemoryUnavailable() {
        server.expect(requestTo(BANK + "/memories/recall")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.recall("q", List.of(), "low", 100))
                .isInstanceOf(MemoryUnavailableException.class)
                .satisfies(e -> assertThat(((MemoryUnavailableException) e).status()).isEqualTo(500));
    }

    @Test
    void withoutAKeyNothingIsSent() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer noCalls = MockRestServiceServer.bindTo(b).build();
        HindsightClient keyless = new HindsightClient(TestProperties.props("", "llm-key"), b);

        assertThatThrownBy(() -> keyless.recall("q", List.of(), "low", 100))
                .isInstanceOf(MemoryUnavailableException.class)
                .hasMessageContaining("HINDSIGHT_API_KEY");
        noCalls.verify();
    }

    @Test
    void bankSettingsFallBackFromPatchToPut() {
        server.expect(requestTo(BANK)).andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.disposition_skepticism").value(4))
                .andRespond(withStatus(HttpStatus.METHOD_NOT_ALLOWED));
        server.expect(requestTo(BANK)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.disposition.skepticism").value(4))
                .andRespond(withSuccess());

        assertThat(client.configureBank("name", "mission", 4, 4, 2)).isTrue();
        server.verify();
    }

    @Test
    void deletingADocumentThatIsAlreadyGoneIsFine() {
        server.expect(requestTo(BANK + "/documents/WARN-55")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.deleteDocument("WARN-55");
        server.verify();
    }

    @Test
    void directiveNamesAreReadFromAListOrAWrappedList() {
        server.expect(requestTo(BANK + "/directives"))
                .andRespond(withSuccess("{\"items\": [{\"name\": \"Cite record IDs\"}]}", MediaType.APPLICATION_JSON));

        assertThat(client.directiveNames()).containsExactly("Cite record IDs");
    }
}
