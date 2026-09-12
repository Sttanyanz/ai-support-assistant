package io.github.Sttanyanz.ai_support_assistant.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Dialog {

    private static final int MAX_CLARIFICATIONS = 2;

    private String id;
    private String userContact;
    private MessageSource source;
    private List<Message> history;
    private DialogStatus status;
    private String pendingQuestion;
    private int clarificationAttempts;
    private String category;
    private String priority;
    private String ticketId;
    private LocalDateTime createdAt;

    public static Dialog newDialog(String userContact, MessageSource source) {
        Dialog d = new Dialog();
        d.setId(UUID.randomUUID().toString());
        d.setUserContact(userContact);
        d.setSource(source);
        d.setHistory(new ArrayList<>());
        d.setStatus(DialogStatus.AWAITING_CLARIFICATION);
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    public void addMessage(Message m) {
        if (history == null) history = new ArrayList<>();
        history.add(m);
    }

    public boolean canAskMoreQuestions() {
        return clarificationAttempts < MAX_CLARIFICATIONS;
    }
}