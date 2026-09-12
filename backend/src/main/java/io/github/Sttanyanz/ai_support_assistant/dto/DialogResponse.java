package io.github.Sttanyanz.ai_support_assistant.dto;

import io.github.Sttanyanz.ai_support_assistant.model.Ticket;
import tools.jackson.databind.JsonNode;

public record DialogResponse(
        String dialogId,
        String action,
        String question,
        Ticket ticket
) {

    public static DialogResponse clarification(String dialogId, String question) {
        return new DialogResponse(dialogId, "ASK_CLARIFICATION", question, null);
    }

    public static DialogResponse ticketCreated(String dialogId, Ticket ticket) {
        return new DialogResponse(dialogId, "CREATE_TICKET", null, ticket);
    }
}