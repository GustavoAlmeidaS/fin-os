package io.github.gustavoalmeidas.finos.ai.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OllamaClient {

    private final RestClient restClient;
    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "llama3:8b";
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>[\\s\\S]*?</think>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[\\s*\\{.*}\\s*]", Pattern.DOTALL);

    public OllamaClient(RestClient.Builder restClientBuilder) {
        // Configure timeout (15 min for slow local models processing large batches)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofMinutes(15));

        this.restClient = restClientBuilder
                .baseUrl(OLLAMA_URL)
                .requestFactory(factory)
                .build();
    }

    /**
     * Generates AI advice using the /api/chat endpoint with separate system and user messages.
     * This provides much better instruction-following compared to /api/generate.
     */
    public String generateAdvice(String systemPrompt, String userPrompt) {
        try {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
            );

            OllamaChatRequest request = new OllamaChatRequest(MODEL_NAME, messages, false, null);

            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response != null && response.message() != null && response.message().content() != null) {
                return cleanResponse(response.message().content());
            }
            return "Não foi possível gerar uma resposta. Resposta vazia da IA.";
        } catch (Exception e) {
            return "Erro ao conectar com a IA local (Ollama). Verifique se o Ollama está rodando e o modelo '"
                    + MODEL_NAME + "' está instalado.\n\nExecute: ollama pull " + MODEL_NAME
                    + "\n\nDetalhes: " + e.getMessage();
        }
    }

    /**
     * Generates a JSON response from Ollama, requesting structured JSON output.
     */
    public String generateJson(String systemPrompt, String userPrompt) {
        try {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
            );

            OllamaChatRequest request = new OllamaChatRequest(MODEL_NAME, messages, false, "json");

            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response != null && response.message() != null && response.message().content() != null) {
                return extractJsonArray(cleanResponse(response.message().content()));
            }
            return "[]";
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Ollama JSON endpoint: " + e.getMessage(), e);
        }
    }

    /**
     * Removes <think>...</think> reasoning blocks from DeepSeek-R1 style models,
     * markdown code fences, and cleans up excessive whitespace.
     */
    private String cleanResponse(String raw) {
        String cleaned = THINK_PATTERN.matcher(raw).replaceAll("");

        // Remove markdown JSON code blocks if present
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        // Remove leading/trailing whitespace and collapse multiple blank lines
        cleaned = cleaned.strip();
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned;
    }

    /**
     * Extracts a JSON array from a potentially wrapped response.
     * Handles cases where the model returns { "suggestions": [...] } instead of just [...].
     */
    private String extractJsonArray(String input) {
        input = input.strip();

        // Already a valid JSON array
        if (input.startsWith("[")) {
            return input;
        }

        // Try to extract array from within a JSON object
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }

        // Fallback: return empty array
        return "[]";
    }

    // --- DTOs for /api/chat ---
    public record ChatMessage(String role, String content) {}
    public record OllamaChatRequest(String model, List<ChatMessage> messages, boolean stream, String format) {}
    public record OllamaChatResponse(String model, ChatMessage message, boolean done,
                                     @JsonProperty("total_duration") Long totalDuration) {}
}
