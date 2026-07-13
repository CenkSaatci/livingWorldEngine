package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "combat_sessions")
public class CombatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "current_turn_entity_id")
    private UUID currentTurnEntityId;

    @Column(nullable = false)
    private int round = 1;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    protected CombatSession() {}

    public CombatSession(UUID worldId) {
        this.worldId = worldId;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public UUID getCurrentTurnEntityId() { return currentTurnEntityId; }
    public void setCurrentTurnEntityId(UUID v) { this.currentTurnEntityId = v; }
    public int getRound() { return round; }
    public void setRound(int v) { this.round = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant v) { this.endedAt = v; }
}