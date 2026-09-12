package io.github.Sttanyanz.ai_support_assistant.controller;

import io.github.Sttanyanz.ai_support_assistant.service.GigachatAuthService;
import io.github.Sttanyanz.ai_support_assistant.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final GigachatAuthService authService;
    private final LlmService llmService;

    @GetMapping("/health/gigachat")
    public Map<String, Object> checkGigaChat() {
        try {
            String token = authService.getAccessToken();
            return Map.of(
                    "status", "OK",
                    "tokenPreview", token.substring(0, 10) + "...",
                    "tokenLength", token.length()
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            );
        }
    }

    @GetMapping("/health/llm")
    public Object checkLlm() {
        try {
            return llmService.analyze("Не могу зайти в личный кабинет, пишет неверный пароль");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }
}