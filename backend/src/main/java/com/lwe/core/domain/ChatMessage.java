package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Persistierte Welt-Chat-Nachrichten (B5: Historie, letzte 50 ladbar). */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_world",
           columnList = "world_id, created_at DESC")
})
public class ChatMessage {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 100)
    private String sender = "Player";

    @Column(nullable = false, columnDefinition = "text")
    private String text = "";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}

    public ChatMessage(UUID worldId, String sender, String text) {
        this.worldId = worldId;
        this.sender = sender;
        this.text = text;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getSender() { return sender; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
