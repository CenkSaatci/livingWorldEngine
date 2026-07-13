package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "npc_intents")
public class NpcIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "npc_id", nullable = false)
    private UUID npcId;

    @Column(name = "intent_type", nullable = false, length = 50)
    private String intentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", nullable = false)
    private String paramsJson;

    @Column(columnDefinition = "text")
    private String reasoning;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "validated_at")
    private Instant validatedAt;

    protected NpcIntent() {}

    public NpcIntent(UUID worldId, UUID npcId, String intentType, String paramsJson, String reasoning) {
        this.worldId = worldId;
        this.npcId = npcId;
        this.intentType = intentType;
        this.paramsJson = paramsJson;
        this.reasoning = reasoning;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public UUID getNpcId() { return npcId; }
    public String getIntentType() { return intentType; }
    public String getParamsJson() { return paramsJson; }
    public String getReasoning() { return reasoning; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Instant v) { this.validatedAt = v; }
}
