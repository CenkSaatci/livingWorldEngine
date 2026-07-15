package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "factions")
public class Faction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 7)
    private String color = "#888888";

    @Column(name = "leader_entity_id")
    private UUID leaderEntityId;

    @Column(name = "founded_at", nullable = false)
    private Instant foundedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Faction() {}

    public Faction(UUID worldId, String name) {
        this.worldId = worldId;
        this.name = name;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }
    public UUID getLeaderEntityId() { return leaderEntityId; }
    public void setLeaderEntityId(UUID v) { this.leaderEntityId = v; }
    public Instant getFoundedAt() { return foundedAt; }
    public void setFoundedAt(Instant v) { this.foundedAt = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
