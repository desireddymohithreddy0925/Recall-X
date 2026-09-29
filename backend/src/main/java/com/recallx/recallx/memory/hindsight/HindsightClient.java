package com.recallx.recallx.memory.hindsight;

import com.recallx.recallx.config.RecallxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Thin client for the Hindsight REST API (there is no Java SDK). All paths are under /v1/default/banks/{bank}:
 * <pre>
 *   POST   {retainPath}                 retain: /memories (Cloud cURL examples) or /memories/retain (API reference)
 *   POST   /memories/recall             recall: raw evidence the code checks
 *   POST   /reflect                     reflect: an answer with sources, for people to read
 *   PATCH  (bank)                       mission and disposition
 *   GET    /directives, POST /directives, POST /mental-models, DELETE /documents/{id}
 * </pre>
 * Every failure becomes {@link MemoryUnavailableException}; callers decide what degrades and what fails.
 */
@Component
public class HindsightClient {

    private static final Logger log = LoggerFactory.getLogger(HindsightClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() { };
    private static final String BANK = "/v1/default/banks/{bank}";

    private final RestClient http;
    private final RecallxProperties.Hindsight props;

    @Autowired
    public HindsightClient(RecallxProperties props) {
        this(props, RestClient.builder().requestFactory(requestFactory(props.hindsight())));
    }

    /** Tests pass a builder bound to MockRestServiceServer. */
    public HindsightClient(RecallxProperties props, RestClient.Builder builder) {
        this.props = props.hindsight();
        builder.baseUrl(stripTrailingSlash(this.props.baseUrl()));
        if (!this.props.apiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + this.props.apiKey());
        }
        this.http = builder.build();
    }

    public String bankId() {
        return props.bankId();
    }

    // ------------------------------------------------------------------ bank setup

    /**
     * Sets the bank's mission and disposition. Tries the API reference's PATCH with flat fields, then the older
     * PUT with a nested disposition. Returns false if neither works; set them in the Hindsight Cloud UI instead.
     */
    public boolean configureBank(String name, String mission, int skepticism, int literalism, int empathy) {
        Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("reflect_mission", mission);
        flat.put("disposition_skepticism", skepticism);
        flat.put("disposition_literalism", literalism);
        flat.put("disposition_empathy", empathy);
        try {
            call("configure bank (PATCH)", () -> http.patch().uri(BANK, bankId())
                    .contentType(MediaType.APPLICATION_JSON).body(flat).retrieve().toBodilessEntity());
            return true;
        } catch (MemoryUnavailableException patchFailed) {
            Map<String, Object> nested = new LinkedHashMap<>();
            nested.put("name", name);
            nested.put("mission", mission);
            nested.put("disposition", Map.of("skepticism", skepticism, "literalism", literalism, "empathy", empathy));
            try {
                call("configure bank (PUT)", () -> http.put().uri(BANK, bankId())
                        .contentType(MediaType.APPLICATION_JSON).body(nested).retrieve().toBodilessEntity());
                return true;
            } catch (MemoryUnavailableException putFailed) {
                log.warn("[hindsight] could not set the mission and disposition of bank {}; set them in the Cloud UI",
                        bankId());
                return false;
            }
        }
    }

    /** Names of the bank's directives. The response may be a list or an object wrapping one. */
    public Set<String> directiveNames() {
        Object response = call("list directives", () -> http.get().uri(BANK + "/directives", bankId())
                .retrieve().body(Object.class));
        Set<String> names = new LinkedHashSet<>();
        for (Object row : rows(response, "directives", "items")) {
            if (row instanceof Map<?, ?> m && m.get("name") != null) names.add(m.get("name").toString());
        }
        return names;
    }

    public void createDirective(String name, String content) {
        call("create directive " + name, () -> http.post().uri(BANK + "/directives", bankId())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", name, "content", content))
                .retrieve().toBodilessEntity());
    }

    /** Creates a mental model that refreshes after consolidation. Returns false if it already exists. */
    public boolean createMentalModel(String id, String name, String sourceQuery) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("name", name);
        body.put("source_query", sourceQuery);
        body.put("trigger", Map.of("refresh_after_consolidation", true));
        try {
            call("create mental model " + id, () -> http.post().uri(BANK + "/mental-models", bankId())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity());
            return true;
        } catch (MemoryUnavailableException e) {
            if (e.status() == 409) return false;
            if (e.status() == 400 || e.status() == 422) {   // custom IDs may not be accepted
                body.remove("id");
                call("create mental model " + name, () -> http.post().uri(BANK + "/mental-models", bankId())
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity());
                return true;
            }
            throw e;
        }
    }

    /** Removes a document and the memories extracted from it. A document that is already gone is not an error. */
    public void deleteDocument(String documentId) {
        try {
            call("delete document " + documentId, () -> http.delete()
                    .uri(BANK + "/documents/{doc}", bankId(), documentId).retrieve().toBodilessEntity());
        } catch (MemoryUnavailableException e) {
            if (e.status() != 404) throw e;
        }
    }

    // ------------------------------------------------------------------ retain, recall, reflect

    /** Retains items. Returns the operation ID when async, otherwise null. */
    public String retain(List<RetainItem> items, boolean async) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items.stream().map(RetainItem::toJson).toList());
        body.put("async", async);
        Map<String, Object> response = call("retain " + items.size() + " item(s)", () ->
                http.post().uri(BANK + props.retainPath(), bankId())
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JSON_MAP));
        for (RetainItem item : items) {
            log.info("[retain] {} queued", item.documentId());
        }
        Object operationId = response == null ? null : response.get("operation_id");
        return operationId == null ? null : operationId.toString();
    }

    public List<RecallHit> recall(String query, List<String> types, String budget, int maxTokens) {
        return recall(RecallRequest.of(query, types, budget, maxTokens)).hits();
    }

    public RecallResponse recall(RecallRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        if (!request.types().isEmpty()) body.put("types", request.types());
        body.put("budget", request.budget());
        body.put("max_tokens", request.maxTokens());
        if (request.minReranker() != null) body.put("min_scores", Map.of("reranker", request.minReranker()));
        if (request.includeSourceFacts()) body.put("include", Map.of("source_facts", Map.of()));
        Map<String, Object> response = call("recall", () ->
                http.post().uri(BANK + "/memories/recall", bankId())
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JSON_MAP));

        List<RecallHit> hits = new ArrayList<>();
        if (response != null && response.get("results") instanceof List<?> results) {
            for (Object result : results) {
                if (result instanceof Map<?, ?> m) hits.add(hit(m));
            }
        }
        Map<String, RecallHit> sourceFacts = new LinkedHashMap<>();
        if (response != null && response.get("source_facts") instanceof Map<?, ?> facts) {
            facts.forEach((id, fact) -> {
                if (fact instanceof Map<?, ?> m) sourceFacts.put(String.valueOf(id), hit(m));
            });
        }
        return new RecallResponse(hits, sourceFacts);
    }

    /** Reflect always asks for based_on, so answers can show their sources. responseSchema may be null. */
    @SuppressWarnings("unchecked")
    public ReflectAnswer reflect(String query, String budget, Map<String, Object> responseSchema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("budget", budget);
        body.put("include", Map.of("facts", Map.of()));
        if (responseSchema != null) body.put("response_schema", responseSchema);
        Map<String, Object> response = call("reflect", () ->
                http.post().uri(BANK + "/reflect", bankId())
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JSON_MAP));
        if (response == null) {
            throw new MemoryUnavailableException("reflect returned an empty response", null);
        }
        Map<String, Object> structured = response.get("structured_output") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : null;
        Integer tokens = response.get("usage") instanceof Map<?, ?> usage && usage.get("total_tokens") instanceof Number n
                ? n.intValue() : null;
        if (tokens != null) log.info("[hindsight] reflect used {} tokens", tokens);
        return new ReflectAnswer(str(response.get("text")), structured, str(response.get("structured_output_error")),
                basedOn(response.get("based_on")), tokens);
    }

    // ------------------------------------------------------------------ helpers

    private <T> T call(String operation, Supplier<T> request) {
        if (props.apiKey().isBlank()) {
            throw new MemoryUnavailableException(operation + " skipped: HINDSIGHT_API_KEY is not set", 401, null);
        }
        long start = System.nanoTime();
        try {
            T result = request.get();
            log.info("[hindsight] {} ok in {} ms", operation, (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String hint = switch (status) {
                case 401, 403 -> " (check HINDSIGHT_API_KEY)";
                case 402 -> " (out of Hindsight credits)";
                case 404 -> " (unknown bank or path)";
                case 429 -> " (rate limited)";
                default -> "";
            };
            String body = e.getResponseBodyAsString();
            log.warn("[hindsight] {} failed after {} ms: HTTP {}{} {}", operation,
                    (System.nanoTime() - start) / 1_000_000, status, hint, abbreviate(body));
            throw new MemoryUnavailableException(operation + " failed: HTTP " + status + hint, status, e);
        } catch (RestClientException e) {
            log.warn("[hindsight] {} failed after {} ms: {}", operation, (System.nanoTime() - start) / 1_000_000,
                    e.getMessage());
            throw new MemoryUnavailableException(operation + " failed: " + e.getMessage(), 0, e);
        }
    }

    private static RecallHit hit(Map<?, ?> m) {
        Double reranker = null;
        Double fin = null;
        if (m.get("scores") instanceof Map<?, ?> scores) {
            reranker = number(scores.get("reranker"));
            fin = number(scores.get("final"));
        }
        List<String> sources = new ArrayList<>();
        if (m.get("source_fact_ids") instanceof List<?> ids) ids.forEach(id -> sources.add(String.valueOf(id)));
        return new RecallHit(str(m.get("id")), str(m.get("text")), str(m.get("type")), blankToNull(str(m.get("document_id"))),
                blankToNull(str(m.get("occurred_start"))), reranker, fin, sources);
    }

    private static BasedOn basedOn(Object value) {
        if (!(value instanceof Map<?, ?> m)) return BasedOn.empty();
        List<BasedOn.Memory> memories = new ArrayList<>();
        for (Object row : rows(m.get("memories"))) {
            if (row instanceof Map<?, ?> r) memories.add(new BasedOn.Memory(str(r.get("id")), str(r.get("text")), str(r.get("type"))));
        }
        List<BasedOn.Ref> models = new ArrayList<>();
        for (Object row : rows(m.get("mental_models"))) {
            if (row instanceof Map<?, ?> r) models.add(new BasedOn.Ref(str(r.get("id")), blankToNull(str(r.get("name"))), str(r.get("text"))));
        }
        List<BasedOn.Ref> directives = new ArrayList<>();
        for (Object row : rows(m.get("directives"))) {
            if (row instanceof Map<?, ?> r) directives.add(new BasedOn.Ref(str(r.get("id")), blankToNull(str(r.get("name"))), str(r.get("content"))));
        }
        return new BasedOn(memories, models, directives);
    }

    /** A JSON list, or the list inside an object under one of the given keys. */
    private static List<?> rows(Object value, String... wrapperKeys) {
        if (value instanceof List<?> list) return list;
        if (value instanceof Map<?, ?> m) {
            for (String key : wrapperKeys) {
                if (m.get(key) instanceof List<?> list) return list;
            }
        }
        return List.of();
    }

    private static JdkClientHttpRequestFactory requestFactory(RecallxProperties.Hindsight props) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }

    private static Double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String abbreviate(String body) {
        if (body == null) return "";
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
