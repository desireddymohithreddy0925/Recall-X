package com.recallx.recallx.llm;

import com.recallx.recallx.TestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BaselineLlmClientTest {

    private static final String ANSWER = "{\"choices\": [{\"message\": {\"content\": \"- Restart the service\"}}]}";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final BaselineLlmClient client = new BaselineLlmClient(TestProperties.props(), builder);

    @Test
    void usesTheSharedRoleAndFormatWithNoTools() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.messages[0].content").value(Prompts.baselineSystem()))
                .andExpect(jsonPath("$.messages[1].content").value("Timeouts?"))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        BaselineLlmClient.LlmAnswer answer = client.answer("Timeouts?");

        assertThat(answer.text()).isEqualTo("- Restart the service");
        assertThat(answer.model()).isEqualTo("openai/gpt-oss-120b");
    }

    @Test
    void aRateLimitRetriesOnceWithTheFallbackModelAndSaysSo() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("qwen/qwen3-32b"))
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(client.answer("Timeouts?").model()).isEqualTo("qwen/qwen3-32b");
        server.verify();
    }

    @Test
    void anOverloadedModelAlsoFallsBack() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("qwen/qwen3-32b"))
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(client.answer("Timeouts?").model()).isEqualTo("qwen/qwen3-32b");
        server.verify();
    }
}
