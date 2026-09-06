package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_relationships", uniqueConstraints =
    @UniqueConstraint(columnNames = {"entity_a_id", "entity_b_id"}))
public class EntityRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_a_id", nullable = false)
    private UUID entityAId;

    @Column(name = "entity_b_id", nullable = false)
    private UUID entityBId;

    @Column(nullable = false, length = 50)
    private String relationship = "neutral";

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

    protected EntityRelationship() {}

    public EntityRelationship(UUID entityAId, UUID entityBId, String relationship) {
        this.entityAId = entityAId;
        this.entityBId = entityBId;
        this.relationship = relationship;
    }

    @PrePersist @PreUpdate void onUpdate() { this.modifiedAt = Instant.now(); }

    public Long getId() { return id; }
    public UUID getEntityAId() { return entityAId; }
    public UUID getEntityBId() { return entityBId; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String v) { this.relationship = v; }
    public Instant getModifiedAt() { return modifiedAt; }
}
