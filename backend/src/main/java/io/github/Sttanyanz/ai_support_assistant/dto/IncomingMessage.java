package io.github.Sttanyanz.ai_support_assistant.dto;

import io.github.Sttanyanz.ai_support_assistant.model.MessageSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncomingMessage(
        @NotNull(message = "Источник сообщения обязателен")
        MessageSource source,

        @NotBlank(message = "Контакт пользователя обязателен")
        String userContact,

        @NotBlank(message = "Текст сообщения не может быть пустым")
        String text
) {}