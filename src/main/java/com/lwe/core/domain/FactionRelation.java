package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faction_relations")
public class FactionRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "faction_a_id", nullable = false)
    private UUID factionAId;

    @Column(name = "faction_b_id", nullable = false)
    private UUID factionBId;

    @Column(name = "relation_status", nullable = false, length = 20)
    private String relationStatus = "NEUTRAL";

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected FactionRelation() {}

    public FactionRelation(UUID factionAId, UUID factionBId, String relationStatus) {
        this.factionAId = factionAId;
        this.factionBId = factionBId;
        this.relationStatus = relationStatus;
    }

    @PrePersist @PreUpdate void onUpdate() { this.changedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getFactionAId() { return factionAId; }
    public UUID getFactionBId() { return factionBId; }
    public String getRelationStatus() { return relationStatus; }
    public void setRelationStatus(String v) { this.relationStatus = v; }
    public Instant getChangedAt() { return changedAt; }
}
