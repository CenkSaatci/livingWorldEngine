package com.lwe.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Ein Regelwerk (Game System) definiert Attribute, Würfel-Mechaniken und Kampfregeln
 * für eine Spielwelt. Die eigentliche Konfiguration liegt in {@code rules_json}.
 *
 * <p>Jedes {@code rules_json} muss gegen das JSON-Schema in {@code schema_json}
 * validieren. Siehe {@code docs/RULES-SCHEMA.md}.
 */
@Entity
@Table(name = "game_systems")
public class GameSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int version = 1;

    @JsonProperty("rules_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_json", nullable = false)
    private String rulesJson;

    @JsonProperty("schema_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Column(name = "owner_id")
    private UUID ownerId;

    /** PRIVATE | INVITE_ONLY | PUBLIC (ADR-011). Legacy-Systeme: PUBLIC, Owner NULL. */
    @Column(nullable = false, length = 20)
    private String visibility = "PRIVATE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GameSystem() {}

    /** Legacy/Seed: kein Owner, global sichtbar (nur Admin schreibbar). */
    public GameSystem(String name, int version, String rulesJson, String schemaJson) {
        this(name, version, rulesJson, schemaJson, null);
        this.visibility = "PUBLIC";
    }

    public GameSystem(String name, int version, String rulesJson, String schemaJson, UUID ownerId) {
        this.name = name;
        this.version = version;
        this.rulesJson = rulesJson;
        this.schemaJson = schemaJson;
        this.ownerId = ownerId;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getRulesJson() { return rulesJson; }
    public void setRulesJson(String v) { this.rulesJson = v; }
    public String getSchemaJson() { return schemaJson; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID v) { this.ownerId = v; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String v) { this.visibility = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}