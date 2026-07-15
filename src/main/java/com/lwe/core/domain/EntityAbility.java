package com.lwe.core.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_abilities")
@IdClass(EntityAbilityId.class)
public class EntityAbility {

    @Id
    @Column(name = "entity_id")
    private UUID entityId;

    @Id
    @Column(name = "ability_id")
    private UUID abilityId;

    @Column(nullable = false)
    private boolean unlocked = true;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    protected EntityAbility() {}

    public EntityAbility(UUID entityId, UUID abilityId) {
        this.entityId = entityId;
        this.abilityId = abilityId;
    }

    public UUID getEntityId() { return entityId; }
    public UUID getAbilityId() { return abilityId; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean v) { this.unlocked = v; }
    public Instant getAssignedAt() { return assignedAt; }
}
