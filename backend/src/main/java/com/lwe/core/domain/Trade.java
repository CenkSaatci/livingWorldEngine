package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Handelsangebot zwischen zwei Entities (B4): Angebot/Gegenangebot + Annahme. */
@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trades_world", columnList = "world_id, status"),
    @Index(name = "idx_trades_participants", columnList = "proposer_entity_id, partner_entity_id")
})
public class Trade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "proposer_entity_id", nullable = false)
    private UUID proposerEntityId;

    @Column(name = "partner_entity_id", nullable = false)
    private UUID partnerEntityId;

    @Column(name = "offer_json", nullable = false, columnDefinition = "text")
    private String offerJson = "[]";

    @Column(name = "request_json", nullable = false, columnDefinition = "text")
    private String requestJson = "[]";

    @Column(nullable = false, length = 20)
    private String status = "proposed";

    @Column(name = "last_editor_entity_id", nullable = false)
    private UUID lastEditorEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Trade() {}

    public Trade(UUID worldId, UUID proposerEntityId, UUID partnerEntityId,
                 String offerJson, String requestJson) {
        this.worldId = worldId;
        this.proposerEntityId = proposerEntityId;
        this.partnerEntityId = partnerEntityId;
        this.offerJson = offerJson;
        this.requestJson = requestJson;
        this.lastEditorEntityId = proposerEntityId;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public UUID getProposerEntityId() { return proposerEntityId; }
    public UUID getPartnerEntityId() { return partnerEntityId; }
    public String getOfferJson() { return offerJson; }
    public void setOfferJson(String v) { this.offerJson = v; }
    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String v) { this.requestJson = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getLastEditorEntityId() { return lastEditorEntityId; }
    public void setLastEditorEntityId(UUID v) { this.lastEditorEntityId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = Instant.now(); }
}
