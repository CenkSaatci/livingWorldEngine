package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "node_choices")
public class NodeChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "target_node_id")
    private UUID targetNodeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_check")
    private String skillCheck;

    @Column(name = "on_success_node_id")
    private UUID onSuccessNodeId;

    @Column(name = "on_failure_node_id")
    private UUID onFailureNodeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected NodeChoice() {}

    public NodeChoice(UUID nodeId, String label, UUID targetNodeId) {
        this.nodeId = nodeId;
        this.label = label;
        this.targetNodeId = targetNodeId;
    }

    public UUID getId() { return id; }
    public UUID getNodeId() { return nodeId; }
    public String getLabel() { return label; }
    public UUID getTargetNodeId() { return targetNodeId; }
    public String getSkillCheck() { return skillCheck; }
    public void setSkillCheck(String v) { this.skillCheck = v; }
    public UUID getOnSuccessNodeId() { return onSuccessNodeId; }
    public void setOnSuccessNodeId(UUID v) { this.onSuccessNodeId = v; }
    public UUID getOnFailureNodeId() { return onFailureNodeId; }
    public void setOnFailureNodeId(UUID v) { this.onFailureNodeId = v; }
}