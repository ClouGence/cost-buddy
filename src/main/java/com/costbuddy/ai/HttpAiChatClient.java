package com.costbuddy.ai;

import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.domain.AiEngineDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpAiChatClient implements AiChatClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient   httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;

    public HttpAiChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(AiEngineDO aiEngine, String systemPrompt, String userPrompt) {
        try {
            String body = objectMapper.writeValueAsString(requestBody(aiEngine, systemPrompt, userPrompt));
            HttpRequest request = HttpRequest.newBuilder(chatCompletionUri(aiEngine.getApiAddr()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiEngine.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("AI_CHAT_FAILED", "ai chat failed with http status " + response.statusCode() + ": " + abbreviate(response.body()));
            }
            return extractContent(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("AI_CHAT_FAILED", "ai chat failed: " + exception.getMessage());
        }
    }

    private Map<String, Object> requestBody(AiEngineDO aiEngine, String systemPrompt, String userPrompt) {
        return Map.of("model", aiEngine.getModel(), "temperature", 0.1, "messages", List
            .of(Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", userPrompt)));
    }

    private URI chatCompletionUri(String apiAddr) {
        String normalized = apiAddr.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        if (isDeepSeekBaseUrl(normalized) || normalized.endsWith("/v1")) {
            return URI.create(normalized + "/chat/completions");
        }
        return URI.create(normalized + "/v1/chat/completions");
    }

    private boolean isDeepSeekBaseUrl(String normalizedApiAddr) {
        try {
            URI uri = new URI(normalizedApiAddr);
            return "api.deepseek.com".equalsIgnoreCase(uri.getHost()) && (uri.getPath() == null || uri.getPath().isBlank());
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private String extractContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        JsonNode text = root.path("choices").path(0).path("text");
        if (text.isTextual()) {
            return text.asText();
        }
        throw new BusinessException("AI_CHAT_FAILED", "ai chat response content is empty: " + abbreviate(responseBody));
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "<empty response>";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 1000) {
            return compact;
        }
        return compact.substring(0, 1000) + "...";
    }
}
