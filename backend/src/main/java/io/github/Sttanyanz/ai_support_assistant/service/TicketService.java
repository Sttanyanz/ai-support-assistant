package io.github.Sttanyanz.ai_support_assistant.service;

import io.github.Sttanyanz.ai_support_assistant.model.Dialog;
import io.github.Sttanyanz.ai_support_assistant.model.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TicketService {

    // In-memory хранилище. Для хакатона достаточно — данные живут до перезапуска.
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public Ticket create(Dialog dialog, String category, String priority) {
        Ticket ticket = Ticket.from(dialog, category, priority);
        tickets.put(ticket.getId(), ticket);
        log.info("Создана заявка id={}, category={}, priority={}", ticket.getId(), category, priority);
        return ticket;
    }

    public List<Ticket> findAll() {
        return tickets.values().stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .toList();
    }

    public Ticket findById(String id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new IllegalArgumentException("Заявка не найдена: " + id);
        }
        return ticket;
    }

    public int count() {
        return tickets.size();
    }
}