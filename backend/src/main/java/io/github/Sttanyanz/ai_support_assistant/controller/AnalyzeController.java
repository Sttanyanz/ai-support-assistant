package io.github.Sttanyanz.ai_support_assistant.controller;

import io.github.Sttanyanz.ai_support_assistant.dto.DialogResponse;
import io.github.Sttanyanz.ai_support_assistant.dto.IncomingMessage;
import io.github.Sttanyanz.ai_support_assistant.model.Dialog;
import io.github.Sttanyanz.ai_support_assistant.model.Ticket;
import io.github.Sttanyanz.ai_support_assistant.service.DialogService;
import io.github.Sttanyanz.ai_support_assistant.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyzeController {

    private final DialogService dialogService;
    private final TicketService ticketService;

    @PostMapping("/messages")
    public DialogResponse receiveMessage(@Valid @RequestBody IncomingMessage msg) {
        return dialogService.handleIncoming(msg);
    }

    @GetMapping("/tickets")
    public List<Ticket> getTickets() {
        return ticketService.findAll();
    }

    @GetMapping("/tickets/{id}")
    public Ticket getTicket(@PathVariable String id) {
        return ticketService.findById(id);
    }

    @GetMapping("/dialogs")
    public List<Dialog> getDialogs() {
        return dialogService.findAll();
    }

    @GetMapping("/dialogs/{id}")
    public Dialog getDialog(@PathVariable String id) {
        return dialogService.findById(id);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "OK",
                "service", "ai-support-assistant",
                "timestamp", System.currentTimeMillis()
        );
    }
}