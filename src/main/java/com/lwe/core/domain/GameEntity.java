package com.lwe.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Universelle Entity-Tabelle — fasst PCs, NPCs und Fraktionen in einer Tabelle zusammen.
 *
 * <p>Die {@code entity_type}-Spalte dient als Discriminator. Attribute, Inventar und Metadaten
 * liegen in JSONB-Spalten, da sie je nach Regelwerk variieren.
 *
 * @see <a href="../../../../docs/DATA-MODEL.md">docs/DATA-MODEL.md Abschnitt 3.5</a>
 */
@Entity
@Table(name = "entities")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(nullable = false, length = 200)
    private String name;

    @JsonProperty("attributes_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes_json", nullable = false)
    private String attributesJson = "{}";

    @JsonProperty("inventory_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inventory_json", nullable = false)
    private String inventoryJson = "[]";

    @JsonProperty("position_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "position_json")
    private String positionJson;

    @JsonProperty("metadata_json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson = "{}";

    @Column(name = "faction_id")
    private UUID factionId;

    @Column(columnDefinition = "text")
    private String backstory;

    @Column(nullable = false)
    private int age = 30;

    @Column(name = "experience_level", nullable = false, length = 20)
    private String experienceLevel = "green";

    @Column(name = "social_standing", nullable = false, length = 20)
    private String socialStanding = "peasant";

    @Column(name = "experience_points", nullable = false)
    private int experiencePoints = 0;

    @Column(name = "unspent_attribute_points", nullable = false)
    private int unspentAttributePoints = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GameEntity() {}

    public GameEntity(UUID worldId, String entityType, String name) {
        this.worldId = worldId;
        this.entityType = entityType;
        this.name = name;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getEntityType() { return entityType; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String v) { this.attributesJson = v; }
    public String getInventoryJson() { return inventoryJson; }
    public void setInventoryJson(String v) { this.inventoryJson = v; }
    public String getPositionJson() { return positionJson; }
    public void setPositionJson(String v) { this.positionJson = v; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String v) { this.metadataJson = v; }
    public UUID getFactionId() { return factionId; }
    public void setFactionId(UUID v) { this.factionId = v; }
    public String getBackstory() { return backstory; }
    public void setBackstory(String v) { this.backstory = v; }
    public int getAge() { return age; }
    public void setAge(int v) { this.age = v; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String v) { this.experienceLevel = v; }
    public String getSocialStanding() { return socialStanding; }
    public void setSocialStanding(String v) { this.socialStanding = v; }
    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int v) { this.experiencePoints = v; }
    public int getUnspentAttributePoints() { return unspentAttributePoints; }
    public void setUnspentAttributePoints(int v) { this.unspentAttributePoints = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}