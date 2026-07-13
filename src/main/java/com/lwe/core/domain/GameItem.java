package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "items")
public class GameItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id")
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal weight = BigDecimal.ZERO;

    @Column(nullable = false)
    private int value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bonuses_json", nullable = false)
    private String bonusesJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GameItem() {}

    public GameItem(UUID worldId, String name, String type, BigDecimal weight, int value) {
        this.worldId = worldId;
        this.name = name;
        this.type = type;
        this.weight = weight;
        this.value = value;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public BigDecimal getWeight() { return weight; }
    public int getValue() { return value; }
    public String getBonusesJson() { return bonusesJson; }
    public void setBonusesJson(String v) { this.bonusesJson = v; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String v) { this.metadataJson = v; }
}