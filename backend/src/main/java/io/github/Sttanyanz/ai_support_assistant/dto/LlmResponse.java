package io.github.Sttanyanz.ai_support_assistant.dto;

public record LlmResponse(
        String action,     // "ASK_CLARIFICATION" или "CREATE_TICKET"
        String category,   // заполнено при CREATE_TICKET
        String priority,   // заполнено при CREATE_TICKET
        String question    // заполнено при ASK_CLARIFICATION
) {}