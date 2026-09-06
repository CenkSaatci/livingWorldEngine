package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Column(nullable = false, length = 50)
    private String type = "village";

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String history;

    @Column(nullable = false)
    private int population;

    @Column(nullable = false)
    private int wealth = 5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String services = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String factions = "[]";

    @Column(name = "is_capital", nullable = false)
    private boolean isCapital;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "position_json")
    private String positionJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Location() {}

    public Location(UUID regionId, String type, String name) {
        this.regionId = regionId;
        this.type = type;
        this.name = name;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getRegionId() { return regionId; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getHistory() { return history; }
    public void setHistory(String v) { this.history = v; }
    public int getPopulation() { return population; }
    public void setPopulation(int v) { this.population = v; }
    public int getWealth() { return wealth; }
    public void setWealth(int v) { this.wealth = v; }
    public String getServices() { return services; }
    public void setServices(String v) { this.services = v; }
    public String getFactions() { return factions; }
    public void setFactions(String v) { this.factions = v; }
    public boolean isCapital() { return isCapital; }
    public void setCapital(boolean v) { this.isCapital = v; }
    public String getPositionJson() { return positionJson; }
    public void setPositionJson(String v) { this.positionJson = v; }
    public Instant getCreatedAt() { return createdAt; }
}
