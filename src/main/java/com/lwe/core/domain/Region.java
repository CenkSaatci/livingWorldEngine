package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String history;

    @Column(name = "danger_level", nullable = false)
    private int dangerLevel = 1;

    @Column(nullable = false, length = 50)
    private String climate = "temperate";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String resources = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String factions = "[]";

    @Column(nullable = false)
    private int population;

    @Column(name = "capital_id")
    private UUID capitalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "position_json")
    private String positionJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "polygon_points")
    private String polygonPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Region() {}

    public Region(UUID worldId, String name) {
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
    public String getHistory() { return history; }
    public void setHistory(String v) { this.history = v; }
    public int getDangerLevel() { return dangerLevel; }
    public void setDangerLevel(int v) { this.dangerLevel = v; }
    public String getClimate() { return climate; }
    public void setClimate(String v) { this.climate = v; }
    public String getResources() { return resources; }
    public void setResources(String v) { this.resources = v; }
    public String getFactions() { return factions; }
    public void setFactions(String v) { this.factions = v; }
    public int getPopulation() { return population; }
    public void setPopulation(int v) { this.population = v; }
    public UUID getCapitalId() { return capitalId; }
    public void setCapitalId(UUID v) { this.capitalId = v; }
    public String getPositionJson() { return positionJson; }
    public void setPositionJson(String v) { this.positionJson = v; }
    public String getPolygonPoints() { return polygonPoints; }
    public void setPolygonPoints(String v) { this.polygonPoints = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
