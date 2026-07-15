package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "world_maps")
public class WorldMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name = "default";

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false)
    private int width = 1000;

    @Column(nullable = false)
    private int height = 750;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WorldMap() {}

    public WorldMap(UUID worldId) {
        this.worldId = worldId;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public int getWidth() { return width; }
    public void setWidth(int v) { this.width = v; }
    public int getHeight() { return height; }
    public void setHeight(int v) { this.height = v; }
    public Instant getCreatedAt() { return createdAt; }
}
