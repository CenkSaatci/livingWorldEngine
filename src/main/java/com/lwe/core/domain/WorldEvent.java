package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "world_events")
public class WorldEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "event_hash", length = 64)
    private String eventHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WorldEvent() {}

    public WorldEvent(UUID worldId, String eventType, UUID sourceEntityId,
                      UUID targetEntityId, Map<String, Object> payload) {
        this(worldId, eventType, sourceEntityId, targetEntityId, payload, null);
    }

    public WorldEvent(UUID worldId, String eventType, UUID sourceEntityId,
                      UUID targetEntityId, Map<String, Object> payload, String eventHash) {
        this.worldId = worldId;
        this.eventType = eventType;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
        this.payloadJson = toJson(payload);
        this.eventHash = eventHash;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Long getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPayloadJson() { return payloadJson; }
}