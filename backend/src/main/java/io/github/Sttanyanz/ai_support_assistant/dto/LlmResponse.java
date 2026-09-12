package io.github.Sttanyanz.ai_support_assistant.dto;

public record LlmResponse(
        String category,
        String priority,
        boolean missingInfo,
        String missingDetails,
        String draftResponse,
        String action
) {}