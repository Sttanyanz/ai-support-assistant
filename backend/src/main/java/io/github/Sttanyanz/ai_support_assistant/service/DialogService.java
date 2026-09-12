package io.github.Sttanyanz.ai_support_assistant.service;

import io.github.Sttanyanz.ai_support_assistant.dto.DialogResponse;
import io.github.Sttanyanz.ai_support_assistant.dto.IncomingMessage;
import io.github.Sttanyanz.ai_support_assistant.dto.LlmResponse;
import io.github.Sttanyanz.ai_support_assistant.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService {

    private final LlmService llmService;
    private final TicketService ticketService;

    private final Map<String, Dialog> dialogs = new ConcurrentHashMap<>();
    private final Map<String, String> activeByUser = new ConcurrentHashMap<>();

    public DialogResponse handleIncoming(IncomingMessage msg) {
        Dialog dialog = findOrCreate(msg);
        dialog.addMessage(Message.user(msg.source(), msg.text()));

        // Если уже исчерпали лимит уточнений — не спрашиваем модель, а сразу создаём заявку
        if (!dialog.canAskMoreQuestions()) {
            log.info("Лимит уточнений исчерпан для dialogId={}, создаём заявку", dialog.getId());
            return createTicket(dialog, dialog.getCategory(), dialog.getPriority());
        }

        LlmResponse llm = llmService.analyze(dialog);

        return switch (llm.action()) {
            case "ASK_CLARIFICATION" -> askClarification(dialog, llm.question());
            default -> createTicket(dialog, llm.category(), llm.priority());
        };
    }

    private DialogResponse askClarification(Dialog dialog, String question) {
        dialog.setPendingQuestion(question);
        dialog.setStatus(DialogStatus.AWAITING_CLARIFICATION);
        dialog.setClarificationAttempts(dialog.getClarificationAttempts() + 1);
        dialog.addMessage(Message.assistant(question));
        log.info("Диалог id={} спрашивает: {}", dialog.getId(), question);
        return DialogResponse.clarification(dialog.getId(), question);
    }

    private DialogResponse createTicket(Dialog dialog, String category, String priority) {
        if (category == null || category.isBlank()) category = "другое";
        if (priority == null || priority.isBlank()) priority = "MEDIUM";
        Ticket ticket = ticketService.create(dialog, category, priority);
        dialog.setTicketId(ticket.getId());
        dialog.setCategory(category);
        dialog.setPriority(priority);
        dialog.setStatus(DialogStatus.TICKET_CREATED);
        dialog.setPendingQuestion(null);
        activeByUser.remove(dialog.getUserContact());
        log.info("Диалог id={} завершён заявкой id={}", dialog.getId(), ticket.getId());
        return DialogResponse.ticketCreated(dialog.getId(), ticket);
    }

    private Dialog findOrCreate(IncomingMessage msg) {
        String activeId = activeByUser.get(msg.userContact());
        Dialog dialog = (activeId != null) ? dialogs.get(activeId) : null;

        if (dialog == null) {
            dialog = Dialog.newDialog(msg.userContact(), msg.source());
            dialogs.put(dialog.getId(), dialog);
            activeByUser.put(msg.userContact(), dialog.getId());
            log.info("Новый диалог id={}, user={}", dialog.getId(), msg.userContact());
        } else {
            log.info("Продолжение диалога id={}, user={}", dialog.getId(), msg.userContact());
        }
        return dialog;
    }

    public Dialog findById(String id) {
        Dialog d = dialogs.get(id);
        if (d == null) throw new IllegalArgumentException("Диалог не найден: " + id);
        return d;
    }

    public List<Dialog> findAll() {
        return dialogs.values().stream()
                .sorted(Comparator.comparing(Dialog::getCreatedAt).reversed())
                .toList();
    }
}