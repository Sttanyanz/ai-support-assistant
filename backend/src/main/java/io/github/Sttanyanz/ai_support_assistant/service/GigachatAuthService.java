package io.github.Sttanyanz.ai_support_assistant.service;

import io.github.Sttanyanz.ai_support_assistant.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.UUID;

@Slf4j
@Service
public class GigachatAuthService {

    @Value("${gigachat.auth.url}")
    private String authUrl;

    @Value("${gigachat.auth.key}")
    private String authKey;

    @Value("${gigachat.scope}")
    private String scope;

    private final RestClient restClient;
    private String accessToken;
    private long expiresAt;

    public GigachatAuthService(RestClient gigachatRestClient) {
        this.restClient = gigachatRestClient;
        log.info("GigaChatAuthService инициализирован, scope={}", scope);
    }

    public synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < expiresAt) {
            long secondsLeft = (expiresAt - System.currentTimeMillis()) / 1000;
            log.debug("Использую кэшированный токен, осталось {} сек", secondsLeft);
            return accessToken;
        }

        log.info("Запрашиваю новый токен GigaChat");
        log.debug("Auth URL: {}, scope: {}, authKey preview: {}...",
                authUrl, scope, maskSecret(authKey));

        String rquid = UUID.randomUUID().toString();
        log.debug("RqUID: {}", rquid);

        long start = System.currentTimeMillis();
        try {
            TokenResponse response = restClient.post()
                .uri(authUrl)
                .header("Authorization", "Bearer " + authKey)
                .header("RqUID", rquid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("scope=" + scope)
                .retrieve()
                .body(TokenResponse.class);

            long duration = System.currentTimeMillis() - start;

            if (response == null || response.access_token() == null) {
                log.error("GigaChat вернул пустой токен, ответ: {}", response);
                throw new RuntimeException("GigaChat не вернул токен");
            }

            this.accessToken = response.access_token();
            this.expiresAt = System.currentTimeMillis() + 25 * 60 * 1000;

            log.info("Токен получен за {} мс, preview: {}..., длина: {}",
                    duration,
                    maskSecret(accessToken),
                    accessToken.length());

            return accessToken;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Ошибка получения токена GigaChat ({} мс): {}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Маскирует секрет: показывает первые 8 символов, остальное скрывает.
     * Никогда не логируй секреты целиком.
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 12) return "***";
        return secret.substring(0, 8);
    }
}