package io.github.Sttanyanz.ai_support_assistant.service;

import io.github.Sttanyanz.ai_support_assistant.dto.LlmResponse;
import io.github.Sttanyanz.ai_support_assistant.model.Dialog;
import io.github.Sttanyanz.ai_support_assistant.model.Message;
import io.github.Sttanyanz.ai_support_assistant.model.MessageSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
    /**
     * Быстрая проверка LLM без полноценного диалога.
     */
    public LlmResponse analyze(String text) {
        Dialog temp = Dialog.newDialog("health-check", MessageSource.CHAT);
        temp.addMessage(Message.user(MessageSource.CHAT, text));
        return analyze(temp);
    }

    public LlmResponse analyze(Dialog dialog) {
        String token = authService.getAccessToken();
        String prompt = buildPrompt(dialog);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Ты — ассистент техподдержки. Отвечай СТРОГО в формате JSON, без markdown и пояснений."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1
        );

        String raw = gigachatRestClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        log.debug("Raw LLM response: {}", raw);
        return parseResponse(raw);
    }

    private String buildPrompt(Dialog dialog) {
        StringBuilder history = new StringBuilder();
        for (Message m : dialog.getHistory()) {
            String who = (m.role() == Message.Role.USER) ? "Пользователь" : "Ассистент";
            history.append(who).append(": ").append(m.text()).append("\n");
        }

        return """
                Проанализируй диалог пользователя с техподдержкой.

                ДИАЛОГ:
                %s

                ЗАДАЧА: определить, достаточно ли информации для решения проблемы.
                Категории: Wi-Fi, учетные записи, образовательные платформы, оборудование, другое.

                ЖЁСТКИЕ ПРАВИЛА:
                1. Уточнение допустимо ТОЛЬКО если без ответа невозможно решить проблему.
                2. При сомнении — выбирай CREATE_TICKET, а не ASK_CLARIFICATION.
                3. Всегда заполняй priority и category для CREATE_TICKET

                ФОРМАТ ОТВЕТА (строго JSON, без markdown):
                {
                  "action": "ASK_CLARIFICATION" или "CREATE_TICKET",
                  "question": "один короткий вопрос (только если action = ASK_CLARIFICATION)",
                  "category": "категория (только если action = CREATE_TICKET)",
                  "priority": "LOW | MEDIUM | HIGH | CRITICAL"
                }
                """.formatted(history);
    }

    private LlmResponse parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode json = objectMapper.readTree(content);

            return new LlmResponse(
                    json.path("action").asText("CREATE_TICKET"),
                    json.path("category").asText("другое"),
                    json.path("priority").asText("MEDIUM"),
                    json.path("question").asText("")
            );
        } catch (Exception e) {
            log.error("Не удалось разобрать ответ LLM: {}", raw, e);
            throw new RuntimeException("Ошибка разбора ответа GigaChat", e);
        }
    }

}