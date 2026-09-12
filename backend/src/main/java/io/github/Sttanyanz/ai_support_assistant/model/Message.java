package io.github.Sttanyanz.ai_support_assistant.model;

import java.time.LocalDateTime;

public record Message(
        Role role,
        MessageSource source,
        String text,
        LocalDateTime receivedAt
) {
    public enum Role { USER, ASSISTANT }

    public static Message user(MessageSource source, String text) {
        return new Message(Role.USER, source, text, LocalDateTime.now());
    }

    public static Message assistant(String text) {
        return new Message(Role.ASSISTANT, null, text, LocalDateTime.now());
    }
}