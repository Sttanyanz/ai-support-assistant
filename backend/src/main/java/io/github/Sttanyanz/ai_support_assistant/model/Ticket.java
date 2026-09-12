package io.github.Sttanyanz.ai_support_assistant.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Ticket {
    private String id;
    private String dialogId;
    private String userContact;
    private MessageSource source;
    private String text;
    private String category;
    private String priority;
    private LocalDateTime createdAt;

    public static Ticket from(Dialog dialog, String category, String priority) {
        Ticket t = new Ticket();
        t.setId(UUID.randomUUID().toString());
        t.setDialogId(dialog.getId());
        t.setUserContact(dialog.getUserContact());
        t.setSource(dialog.getSource());
        t.setText(dialog.getHistory().stream()
                .filter(m -> m.role() == Message.Role.USER)
                .map(Message::text)
                .reduce((a, b) -> a + " / " + b)
                .orElse(""));
        t.setCategory(category);
        t.setPriority(priority);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}