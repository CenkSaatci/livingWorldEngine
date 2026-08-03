package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "adventure_nodes")
public class AdventureNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "adventure_id", nullable = false)
    private UUID adventureId;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_end", nullable = false)
    private boolean isEnd = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AdventureNode() {}

    public AdventureNode(UUID adventureId, String text, boolean isEnd) {
        this.adventureId = adventureId;
        this.text = text;
        this.isEnd = isEnd;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAdventureId() { return adventureId; }
    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public boolean isEnd() { return isEnd; }
    public void setEnd(boolean v) { this.isEnd = v; }
}