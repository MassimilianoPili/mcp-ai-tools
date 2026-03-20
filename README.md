# MCP AI Tools

Spring Boot starter providing 8 MCP AI-powered text processing tools via local LLM (Ollama-compatible).

Zero external AI API dependencies — all processing runs on your local LLM.

## Installation

```xml
<dependency>
    <groupId>io.github.massimilianopili</groupId>
    <artifactId>mcp-ai-tools</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Tools (8)

| Tool | Description |
|------|-------------|
| `ai_summarize` | Summarizes text (short/medium/long target length) |
| `ai_classify` | Classifies text into user-provided categories with confidence score |
| `ai_extract_entities` | Extracts named entities (persons, organizations, locations, dates) |
| `ai_extract_json` | Extracts structured data matching a user-provided JSON schema |
| `ai_translate` | Translates text to a specified language |
| `ai_sentiment` | Analyzes sentiment (positive/negative/neutral/mixed with score) |
| `ai_keywords` | Extracts key topics and keywords |
| `ai_answer` | Answers questions based on provided context (extractive Q&A) |

## Configuration

| Environment Variable | Property | Default | Description |
|---------------------|----------|---------|-------------|
| `MCP_AI_ENABLED` | `mcp.ai.enabled` | `false` | Enable AI tools |
| `MCP_AI_LLM_URL` | `mcp.ai.llm-url` | *(required)* | Ollama API base URL |
| `MCP_AI_LLM_MODEL` | `mcp.ai.llm-model` | *(required)* | Model name (e.g. `qwen3:8b`, `llama3.1:8b`) |
| `MCP_AI_MAX_INPUT_LENGTH` | `mcp.ai.max-input-length` | `32000` | Max input text length (chars) |
| `MCP_AI_TIMEOUT_SECONDS` | `mcp.ai.timeout-seconds` | `120` | HTTP timeout for LLM calls |

### Example: Ollama

```properties
MCP_AI_ENABLED=true
MCP_AI_LLM_URL=http://ollama:11434
MCP_AI_LLM_MODEL=qwen3:8b
```

## How It Works

- Sends requests to Ollama `/api/generate` endpoint via WebClient
- Each tool has a specialized system prompt for structured output
- JSON parsing with fallback: tries markdown code blocks → raw JSON → plain text
- Temperature set to 0.3 for deterministic, factual outputs
- Input truncated at `maxInputLength` to prevent context overflow

## Output Format

All tools return structured JSON responses. For tools that request JSON output from the LLM (classify, extract_entities, sentiment, keywords, answer), the response is parsed and returned as-is. If JSON parsing fails, the raw LLM response is returned with a `_parseWarning` field.

## Requirements

- Java 21+
- Spring Boot 3.4+
- Spring AI 1.0+
- [spring-ai-reactive-tools](https://github.com/massimilianopili/spring-ai-reactive-tools) 0.3.0+
- An Ollama-compatible LLM server

## License

Apache License 2.0
