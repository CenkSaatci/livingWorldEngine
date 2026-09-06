package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "adventures")
public class Adventure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "giver_entity_id")
    private UUID giverEntityId;

    @Column(name = "start_node_id")
    private UUID startNodeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Adventure() {}

    public Adventure(UUID worldId, String name) {
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
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID v) { this.locationId = v; }
    public UUID getGiverEntityId() { return giverEntityId; }
    public void setGiverEntityId(UUID v) { this.giverEntityId = v; }
    public UUID getStartNodeId() { return startNodeId; }
    public void setStartNodeId(UUID v) { this.startNodeId = v; }
    public Instant getCreatedAt() { return createdAt; }
}