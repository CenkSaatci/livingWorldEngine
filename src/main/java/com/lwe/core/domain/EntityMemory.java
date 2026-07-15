package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_memories")
public class EntityMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    @Column(nullable = false)
    private int sentiment;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "source_event_id")
    private Long sourceEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EntityMemory() {}

    public EntityMemory(UUID entityId, UUID subjectId, String memoryType,
                        int sentiment, String summary) {
        this.entityId = entityId;
        this.subjectId = subjectId;
        this.memoryType = memoryType;
        this.sentiment = sentiment;
        this.summary = summary;
    }

    public Long getId() { return id; }
    public UUID getEntityId() { return entityId; }
    public UUID getSubjectId() { return subjectId; }
    public String getMemoryType() { return memoryType; }
    public int getSentiment() { return sentiment; }
    public void setSentiment(int v) { this.sentiment = v; }
    public String getSummary() { return summary; }
    public Long getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(Long v) { this.sourceEventId = v; }
    public Instant getCreatedAt() { return createdAt; }
}
