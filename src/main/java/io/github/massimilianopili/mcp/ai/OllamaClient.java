package io.github.massimilianopili.mcp.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "mcp.ai.enabled", havingValue = "true")
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);

    private final WebClient webClient;
    private final AiProperties props;

    public OllamaClient(@Qualifier("aiWebClient") WebClient webClient, AiProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @SuppressWarnings("unchecked")
    public Mono<String> generate(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", props.getLlmModel(),
                "prompt", userPrompt,
                "system", systemPrompt,
                "stream", false,
                "options", Map.of("temperature", 0.3, "num_predict", 4096)
        );

        return webClient.post()
                .uri(props.getGenerateEndpoint())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> (String) resp.getOrDefault("response", ""))
                .doOnError(e -> log.warn("Ollama generate failed: {}", e.getMessage()));
    }

    public String extractJson(String response) {
        if (response == null || response.isBlank()) return null;

        // Try markdown code block first
        Matcher blockMatcher = JSON_BLOCK.matcher(response);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        // Try raw JSON object
        Matcher objMatcher = JSON_OBJECT.matcher(response);
        if (objMatcher.find()) {
            return objMatcher.group().trim();
        }

        // Try raw JSON array
        Matcher arrMatcher = JSON_ARRAY.matcher(response);
        if (arrMatcher.find()) {
            return arrMatcher.group().trim();
        }

        return response.trim();
    }

    public String truncateInput(String text) {
        if (text == null) return "";
        if (text.length() <= props.getMaxInputLength()) return text;
        return text.substring(0, props.getMaxInputLength()) + "\n[... truncated at " + props.getMaxInputLength() + " chars]";
    }
}
