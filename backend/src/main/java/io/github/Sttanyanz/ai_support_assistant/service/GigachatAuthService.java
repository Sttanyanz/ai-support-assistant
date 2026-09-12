package io.github.Sttanyanz.ai_support_assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
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

    public GigachatAuthService() throws Exception {
        // Отключаем проверку SSL-сертификата — только для хакатона!
        // GigaChat использует сертификаты НУЦ Минцифры, которых нет в стандартном truststore JDK.
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        }, new SecureRandom());

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();

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

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("scope", scope);

        long start = System.currentTimeMillis();
        try {
            log.info("Scope raw: [{}], length: {}, bytes: {}",
                    scope, scope.length(),
                    java.util.Arrays.toString(scope.getBytes()));
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

    public record TokenResponse(String access_token, long exp) {}
}