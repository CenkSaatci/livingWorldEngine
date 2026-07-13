package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity-spezifisches Ereignis-Log.
 *
 * <p>Jede Region, jeder Ort und jeder NPC bekommt eine Chronik von Ereignissen.
 * Der {@code entity_type}-Discriminator erlaubt entity-spezifische Queries.
 *
 * @see <a href="../../../../docs/WORLD-DEPTH.md">docs/WORLD-DEPTH.md</a>
 */
@Entity
@Table(name = "entity_events", indexes = {
    @Index(name = "idx_entity_events_lookup",
           columnList = "entity_type, entity_id, created_at DESC")
})
public class EntityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int importance = 1;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EntityEvent() {}

    public EntityEvent(String entityType, UUID entityId, String eventType,
                       String title, String description, int importance) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.importance = importance;
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getImportance() { return importance; }
    public UUID getSourceEntityId() { return sourceEntityId; }
    public void setSourceEntityId(UUID v) { this.sourceEntityId = v; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String v) { this.metadataJson = v; }
    public Instant getCreatedAt() { return createdAt; }
}
