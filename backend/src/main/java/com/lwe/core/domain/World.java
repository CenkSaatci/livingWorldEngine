package com.lwe.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worlds")
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "game_system_id")
    private UUID gameSystemId;

    @JsonProperty("settings_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_json", nullable = false)
    private String settingsJson;

    @Column(name = "current_game_time")
    private Instant currentGameTime;

    @Column(name = "last_tick_at")
    private Instant lastTickAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected World() {}

    public World(String name, UUID ownerId, UUID gameSystemId, String settingsJson) {
        this.name = name;
        this.ownerId = ownerId;
        this.gameSystemId = gameSystemId;
        this.settingsJson = settingsJson != null ? settingsJson : "{}";
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getGameSystemId() { return gameSystemId; }
    public void setGameSystemId(UUID v) { this.gameSystemId = v; }
    public String getSettingsJson() { return settingsJson; }
    public Instant getCurrentGameTime() { return currentGameTime; }
    public void setCurrentGameTime(Instant v) { this.currentGameTime = v; }
    public Instant getLastTickAt() { return lastTickAt; }
    public void setLastTickAt(Instant v) { this.lastTickAt = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void setName(String name) { this.name = name; }
    public void setSettingsJson(String s) { this.settingsJson = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}