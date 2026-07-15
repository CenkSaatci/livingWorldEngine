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

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GameSystem() {}

    public GameSystem(String name, int version, String rulesJson, String schemaJson) {
        this.name = name;
        this.version = version;
        this.rulesJson = rulesJson;
        this.schemaJson = schemaJson;
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}