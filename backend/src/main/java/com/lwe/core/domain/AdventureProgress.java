package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "adventure_progress", uniqueConstraints =
    @UniqueConstraint(columnNames = {"adventure_id", "entity_id"}))
public class AdventureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "adventure_id", nullable = false)
    private UUID adventureId;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "current_node_id", nullable = false)
    private UUID currentNodeId;

    @Column(name = "visited_nodes", columnDefinition = "UUID[]")
    private String visitedNodes;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AdventureProgress() {}

    public AdventureProgress(UUID adventureId, UUID entityId, UUID currentNodeId) {
        this.adventureId = adventureId;
        this.entityId = entityId;
        this.currentNodeId = currentNodeId;
        this.visitedNodes = "{" + currentNodeId + "}";
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAdventureId() { return adventureId; }
    public UUID getEntityId() { return entityId; }
    public UUID getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(UUID v) { this.currentNodeId = v; }
    public String getVisitedNodes() { return visitedNodes; }
    public void setVisitedNodes(String v) { this.visitedNodes = v; }
    public void addVisitedNode(UUID nodeId) { this.visitedNodes = this.visitedNodes + "," + nodeId; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getStartedAt() { return startedAt; }
}