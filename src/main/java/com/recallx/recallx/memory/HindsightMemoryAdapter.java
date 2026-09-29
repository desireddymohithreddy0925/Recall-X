package com.recallx.recallx.memory;

import com.recallx.recallx.memory.dto.StructuredExperience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HindsightMemoryAdapter implements OrganizationalMemoryService {
    private static final Logger log = LoggerFactory.getLogger(HindsightMemoryAdapter.class);
    
    private final WebClient webClient;

    public HindsightMemoryAdapter(
            @Value("${hindsight.base-url}") String baseUrl,
            @Value("${hindsight.api-key:}") String apiKey) {
        
        WebClient.Builder builder = WebClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = builder.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Override
    public Optional<String> rememberExperience(StructuredExperience experience) {
        try {
            if (experience == null) return Optional.empty();
            
            String docId = experience.getDocumentId() != null ? experience.getDocumentId() : "hindsight-id-" + System.currentTimeMillis();
            String bankId = "org-" + experience.getOrganizationId();
            
            // Construct a flat text string to represent the experience for Hindsight
            StringBuilder content = new StringBuilder();
            content.append("[").append(docId).append("] ");
            if (experience.getTitle() != null) content.append(experience.getTitle()).append(". ");
            if (experience.getWhatHappened() != null) content.append(experience.getWhatHappened()).append(" ");
            if (experience.getWhyItHappened() != null) content.append(experience.getWhyItHappened()).append(" ");
            if (experience.getWhatFailed() != null) content.append("Failed: ").append(String.join(", ", experience.getWhatFailed())).append(". ");
            if (experience.getWhatWorked() != null) content.append("Worked: ").append(String.join(", ", experience.getWhatWorked())).append(". ");
            if (experience.getLessons() != null) content.append("Lesson: ").append(String.join(", ", experience.getLessons())).append(". ");

            Map<String, Object> body = Map.of(
                "items", List.of(
                    Map.of(
                        "id", docId, // Providing the ID explicitly
                        "content", Map.of("text", content.toString())
                    )
                )
            );

            log.info("Sending experience {} to real Hindsight Server at {}", docId, bankId);
            
            webClient.post()
                    .uri("/v1/default/banks/{bankId}/memories", bankId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(); // Blocking just for simplicity in this interface

            return Optional.of(docId);
        } catch (Exception e) {
            log.error("Real Hindsight API failure while remembering experience: {}", e.getMessage());
            return Optional.empty(); // Application must remain functional if memory provider fails
        }
    }

    @Override
    public List<StructuredExperience> retrieveRelevantMemories(String contextQuery, Long organizationId) {
        try {
            String bankId = "org-" + organizationId;
            log.info("Querying real Hindsight Server for bank: {}", bankId);
            
            Map<String, Object> body = Map.of("query", contextQuery);
            Map response = webClient.post()
                    .uri("/v1/default/banks/{bankId}/recall", bankId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                    
            log.info("Hindsight Recall Response: {}", response);
            // In a full implementation, you would parse the response maps into StructuredExperience
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Real Hindsight API failure while retrieving memories: {}", e.getMessage());
            return Collections.emptyList(); // Graceful degradation
        }
    }

    @Override
    public boolean relateExperiences(String sourceHindsightId, String targetHindsightId) {
        return false;
    }

    @Override
    public List<StructuredExperience> searchHistoricalExperiences(String query, Long organizationId) {
        return retrieveRelevantMemories(query, organizationId);
    }
}
