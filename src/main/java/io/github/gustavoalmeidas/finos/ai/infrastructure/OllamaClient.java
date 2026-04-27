package io.github.gustavoalmeidas.finos.ai.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class OllamaClient {

    private final RestClient restClient;
    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "llama3:8b";
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>[\\s\\S]*?</think>", Pattern.CASE_INSENSITIVE);

    public OllamaClient(RestClient.Builder restClientBuilder) {
        // Configure timeout (120s for slow local models)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));

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

            OllamaChatRequest request = new OllamaChatRequest(MODEL_NAME, messages, false);

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
     * Removes <think>...</think> reasoning blocks from DeepSeek-R1 style models
     * and cleans up excessive whitespace.
     */
    private String cleanResponse(String raw) {
        String cleaned = THINK_PATTERN.matcher(raw).replaceAll("");
        // Remove leading/trailing whitespace and collapse multiple blank lines
        cleaned = cleaned.strip();
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned;
    }

    // --- DTOs for /api/chat ---
    public record ChatMessage(String role, String content) {}
    public record OllamaChatRequest(String model, List<ChatMessage> messages, boolean stream) {}
    public record OllamaChatResponse(String model, ChatMessage message, boolean done,
                                     @JsonProperty("total_duration") Long totalDuration) {}
}
