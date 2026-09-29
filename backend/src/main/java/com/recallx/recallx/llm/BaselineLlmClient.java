package com.recallx.recallx.llm;

import com.recallx.recallx.config.RecallxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "without memory" answer. Uses any OpenAI-compatible chat API (Groq by default; Gemini's OpenAI-compatible
 * endpoint works too). Plain chat with no function calling, which avoids the tool-call errors the organisers warned
 * about. If the model is rate limited (429) or overloaded (503), it retries once with the fallback model, and the
 * answer says which model produced it.
 */
@Component
public class BaselineLlmClient {

    private static final Logger log = LoggerFactory.getLogger(BaselineLlmClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() { };

    private final RestClient http;
    private final RecallxProperties.Llm settings;

    public record LlmAnswer(String text, String model) { }

    @Autowired
    public BaselineLlmClient(RecallxProperties props) {
        this(props, RestClient.builder().requestFactory(requestFactory()));
    }

    /** Tests pass a builder bound to MockRestServiceServer. */
    public BaselineLlmClient(RecallxProperties props, RestClient.Builder builder) {
        this.settings = props.llm();
        String base = settings.baseUrl().endsWith("/")
                ? settings.baseUrl().substring(0, settings.baseUrl().length() - 1) : settings.baseUrl();
        this.http = builder.baseUrl(base).build();
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }

    public LlmAnswer answer(String question) {
        if (settings.apiKey().isBlank()) {
            throw new IllegalStateException("LLM_API_KEY is not set");
        }
        try {
            return new LlmAnswer(complete(settings.model(), question), settings.model());
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException.ServiceUnavailable e) {
            if (settings.fallbackModel().isBlank() || settings.fallbackModel().equals(settings.model())) throw e;
            log.warn("[llm] {} returned {}; retrying once with {}", settings.model(), e.getStatusCode().value(),
                    settings.fallbackModel());
            return new LlmAnswer(complete(settings.fallbackModel(), question), settings.fallbackModel());
        }
    }

    private String complete(String model, String question) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("max_tokens", 1024);   // gpt-oss spends part of this on reasoning
        body.put("messages", List.of(
                Map.of("role", "system", "content", Prompts.baselineSystem()),
                Map.of("role", "user", "content", question)));
        Map<String, Object> response = http.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + settings.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JSON_MAP);
        if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()
                && choices.get(0) instanceof Map<?, ?> choice && choice.get("message") instanceof Map<?, ?> message) {
            Object content = message.get("content");
            if (content != null && !content.toString().isBlank()) return content.toString().trim();
        }
        return "(The model returned no answer.)";
    }
}
