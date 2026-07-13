package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 50)
    private String type = "kill";

    @Column(name = "giver_id")
    private UUID giverId;

    @Column(name = "location_id")
    private UUID locationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String objectives = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String rewards = "{}";

    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Quest() {}

    public Quest(UUID worldId, String title, String type, String objectives, String rewards) {
        this.worldId = worldId;
        this.title = title;
        this.type = type;
        this.objectives = objectives;
        this.rewards = rewards;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getType() { return type; }
    public UUID getGiverId() { return giverId; }
    public void setGiverId(UUID v) { this.giverId = v; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID v) { this.locationId = v; }
    public String getObjectives() { return objectives; }
    public String getRewards() { return rewards; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean v) { this.aiGenerated = v; }
    public Instant getCreatedAt() { return createdAt; }
}
