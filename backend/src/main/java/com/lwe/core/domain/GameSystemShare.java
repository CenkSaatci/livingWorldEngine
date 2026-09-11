package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_system_shares", uniqueConstraints =
    @UniqueConstraint(columnNames = {"system_id", "user_id"}))
public class GameSystemShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "system_id", nullable = false)
    private UUID systemId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected GameSystemShare() {}

    public GameSystemShare(UUID systemId, UUID userId) {
        this.systemId = systemId;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getSystemId() { return systemId; }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
}
