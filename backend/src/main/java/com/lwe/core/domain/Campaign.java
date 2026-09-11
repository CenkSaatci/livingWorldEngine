package com.lwe.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "game_system_id", nullable = false)
    private UUID gameSystemId;

    @Column(nullable = false, length = 200)
    private String name;

    @JsonProperty("settings_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_json", nullable = false)
    private String settingsJson = "{}";

    @JsonProperty("state_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_json", nullable = false)
    private String stateJson = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Campaign() {}

    /** True, wenn worldId eine beim Erstellen erzeugte Fork-Kopie ist (P27-T03). */
    @Column(name = "forked_world", nullable = false)
    private boolean forkedWorld = false;

    /** Pin: rulesJson beim Erstellen/Nachziehen (P27-T05, Option B). */
    @Column(name = "rules_json_snapshot")
    private String rulesJsonSnapshot;

    @Column(name = "game_system_version")
    private Integer gameSystemVersion;

    public Campaign(UUID worldId, UUID gameSystemId, String name) {
        this.worldId = worldId;
        this.gameSystemId = gameSystemId;
        this.name = name;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public UUID getGameSystemId() { return gameSystemId; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getSettingsJson() { return settingsJson; }
    public void setSettingsJson(String v) { this.settingsJson = v; }
    public String getStateJson() { return stateJson; }
    public void setStateJson(String v) { this.stateJson = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isForkedWorld() { return forkedWorld; }
    public void setForkedWorld(boolean v) { this.forkedWorld = v; }

    public String getRulesJsonSnapshot() { return rulesJsonSnapshot; }
    public void setRulesJsonSnapshot(String v) { this.rulesJsonSnapshot = v; }
    public Integer getGameSystemVersion() { return gameSystemVersion; }
    public void setGameSystemVersion(Integer v) { this.gameSystemVersion = v; }
}
