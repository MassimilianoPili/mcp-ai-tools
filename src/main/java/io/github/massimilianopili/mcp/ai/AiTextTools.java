package io.github.massimilianopili.mcp.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@ConditionalOnProperty(name = "mcp.ai.enabled", havingValue = "true")
public class AiTextTools {

    private final OllamaClient ollama;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiTextTools(OllamaClient ollama) {
        this.ollama = ollama;
    }

    @ReactiveTool(name = "ai_summarize",
          description = "Summarizes text using a local LLM. Supports short/medium/long target length.",
          timeoutMs = 180000)
    public Mono<Map<String, Object>> summarize(
            @ToolParam(description = "Text to summarize") String text,
            @ToolParam(description = "Target length: 'short' (1-2 sentences), 'medium' (paragraph), 'long' (detailed). Default: medium", required = false) String length) {
        String target = (length != null && !length.isBlank()) ? length.toLowerCase() : "medium";
        String lengthInstruction = switch (target) {
            case "short" -> "Provide a 1-2 sentence summary.";
            case "long" -> "Provide a detailed summary covering all key points (3-5 paragraphs).";
            default -> "Provide a concise paragraph summary.";
        };

        String system = "You are a precise text summarizer. " + lengthInstruction +
                " Output ONLY the summary, no preamble or labels.";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, "Summarize the following text:\n\n" + input)
                .map(resp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("summary", resp.trim());
                    m.put("targetLength", target);
                    m.put("inputChars", text.length());
                    return m;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Summarization failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_classify",
          description = "Classifies text into one of the provided categories using a local LLM",
          timeoutMs = 120000)
    public Mono<Map<String, Object>> classify(
            @ToolParam(description = "Text to classify") String text,
            @ToolParam(description = "Comma-separated list of categories (e.g. 'bug,feature,question,docs')") String categories) {
        String system = "You are a text classifier. Classify the given text into exactly ONE of these categories: " +
                categories + ". Respond with ONLY a JSON object: {\"category\": \"chosen_category\", \"confidence\": 0.0-1.0, \"reason\": \"brief explanation\"}";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> parseJsonResponse(resp, "classification"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Classification failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_extract_entities",
          description = "Extracts named entities (persons, organizations, locations, dates) from text using a local LLM",
          timeoutMs = 120000)
    public Mono<Map<String, Object>> extractEntities(
            @ToolParam(description = "Text to extract entities from") String text) {
        String system = "You are a named entity extractor. Extract all named entities from the text. " +
                "Respond with ONLY a JSON object: {\"persons\": [...], \"organizations\": [...], \"locations\": [...], \"dates\": [...], \"other\": [...]}. " +
                "Each array contains strings. Use empty arrays if no entities of that type are found.";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> parseJsonResponse(resp, "entities"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Entity extraction failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_extract_json",
          description = "Extracts structured data from text according to a JSON schema provided by the user",
          timeoutMs = 120000)
    public Mono<Map<String, Object>> extractJson(
            @ToolParam(description = "Text to extract data from") String text,
            @ToolParam(description = "JSON schema or example describing the desired output structure (e.g. '{\"title\": \"string\", \"price\": \"number\", \"tags\": [\"string\"]}')") String schema) {
        String system = "You are a structured data extractor. Extract information from the text and return it as a JSON object " +
                "matching this schema: " + schema + ". Respond with ONLY the JSON object, no other text.";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> parseJsonResponse(resp, "extracted"))
                .onErrorResume(e -> Mono.just(Map.of("error", "JSON extraction failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_translate",
          description = "Translates text to the specified language using a local LLM",
          timeoutMs = 180000)
    public Mono<Map<String, Object>> translate(
            @ToolParam(description = "Text to translate") String text,
            @ToolParam(description = "Target language (e.g. 'English', 'Italian', 'French', 'Japanese')") String targetLanguage) {
        String system = "You are a professional translator. Translate the given text to " + targetLanguage +
                ". Output ONLY the translation, no preamble, notes, or labels.";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("translation", resp.trim());
                    m.put("targetLanguage", targetLanguage);
                    m.put("inputChars", text.length());
                    return m;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Translation failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_sentiment",
          description = "Analyzes the sentiment of text (positive/negative/neutral) using a local LLM",
          timeoutMs = 120000)
    public Mono<Map<String, Object>> sentiment(
            @ToolParam(description = "Text to analyze") String text) {
        String system = "You are a sentiment analyzer. Analyze the sentiment of the given text. " +
                "Respond with ONLY a JSON object: {\"sentiment\": \"positive|negative|neutral|mixed\", \"score\": -1.0 to 1.0, \"reason\": \"brief explanation\"}";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> parseJsonResponse(resp, "sentiment"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Sentiment analysis failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_keywords",
          description = "Extracts key topics and keywords from text using a local LLM",
          timeoutMs = 120000)
    public Mono<Map<String, Object>> keywords(
            @ToolParam(description = "Text to extract keywords from") String text,
            @ToolParam(description = "Maximum number of keywords to extract (default: 10)", required = false) Integer maxKeywords) {
        int max = (maxKeywords != null && maxKeywords > 0) ? maxKeywords : 10;
        String system = "You are a keyword extractor. Extract the top " + max + " most important keywords or key phrases from the text. " +
                "Respond with ONLY a JSON object: {\"keywords\": [\"keyword1\", \"keyword2\", ...]}";
        String input = ollama.truncateInput(text);

        return ollama.generate(system, input)
                .map(resp -> parseJsonResponse(resp, "keywords"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Keyword extraction failed: " + e.getMessage())));
    }

    @ReactiveTool(name = "ai_answer",
          description = "Answers a question based on the provided context text using a local LLM (extractive Q&A)",
          timeoutMs = 180000)
    public Mono<Map<String, Object>> answer(
            @ToolParam(description = "Context text containing the information") String context,
            @ToolParam(description = "Question to answer") String question) {
        String system = "You are a question answering system. Answer the question based ONLY on the provided context. " +
                "If the answer is not in the context, say so. " +
                "Respond with ONLY a JSON object: {\"answer\": \"your answer\", \"confidence\": 0.0-1.0, \"relevant_excerpt\": \"quote from context that supports the answer\"}";
        String input = ollama.truncateInput(context);

        return ollama.generate(system, "Context:\n" + input + "\n\nQuestion: " + question)
                .map(resp -> parseJsonResponse(resp, "answer"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Q&A failed: " + e.getMessage())));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response, String fallbackKey) {
        String json = ollama.extractJson(response);
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // If JSON parsing fails, return raw response
            return Map.of(fallbackKey, response.trim(), "_parseWarning", "Response was not valid JSON");
        }
    }
}
