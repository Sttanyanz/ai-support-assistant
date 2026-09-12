package io.github.Sttanyanz.ai_support_assistant.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import io.github.Sttanyanz.ai_support_assistant.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestClient gigachatRestClient;
    private final GigachatAuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gigachat.api.url}")
    private String apiUrl;

    @Value("${gigachat.model}")
    private String model;

    public LlmResponse analyze(String userText) {
        String token = authService.getAccessToken();
        String prompt = buildPrompt(userText);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Ты — помощник техподдержки. Отвечай СТРОГО в формате JSON без markdown."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        long start = System.currentTimeMillis();
        String raw = gigachatRestClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        log.info("GigaChat ответил за {} мс", System.currentTimeMillis() - start);
        log.debug("Raw response: {}", raw);

        return parseResponse(raw);
    }

    private String buildPrompt(String text) {
        return """
                Проанализируй обращение пользователя в техподдержку.

                Обращение:
                \"\"\"%s\"\"\"

                Верни JSON с полями:
                - category: категория проблемы (Wi-Fi, учетные записи, образовательные платформы, оборудование, другое)
                - priority: LOW, MEDIUM или HIGH
                - missingInfo: true, если не хватает данных для решения
                - missingDetails: что именно нужно уточнить (если missingInfo = true)
                - draftResponse: черновик ответа пользователю
                - action: CREATE_TICKET (если всё ясно), ASK_CLARIFICATION (если не хватает данных), DRAFT_REPLY (если нужен только черновик)
                """.formatted(text);
    }

    private LlmResponse parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            // GigaChat иногда оборачивает JSON в ```json ... ```
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode json = objectMapper.readTree(content);
            return new LlmResponse(
                    json.path("category").asText("другое"),
                    json.path("priority").asText("MEDIUM"),
                    json.path("missingInfo").asBoolean(false),
                    json.path("missingDetails").asText(""),
                    json.path("draftResponse").asText(""),
                    json.path("action").asText("CREATE_TICKET")
            );
        } catch (Exception e) {
            log.error("Не удалось разобрать ответ LLM: {}", raw, e);
            throw new RuntimeException("Ошибка разбора ответа GigaChat", e);
        }
    }
}