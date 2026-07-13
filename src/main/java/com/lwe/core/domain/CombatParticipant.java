package com.lwe.core.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "combat_participants", uniqueConstraints =
    @UniqueConstraint(columnNames = {"combat_id", "entity_id"}))
public class CombatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "combat_id", nullable = false)
    private UUID combatId;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private int initiative;

    @Column(name = "ap_current", nullable = false)
    private int apCurrent;

    @Column(name = "ap_max", nullable = false)
    private int apMax;

    @Column(nullable = false, length = 20)
    private String side = "A";

    protected CombatParticipant() {}

    public CombatParticipant(UUID combatId, UUID entityId, int initiative, int apMax, String side) {
        this.combatId = combatId;
        this.entityId = entityId;
        this.initiative = initiative;
        this.apCurrent = apMax;
        this.apMax = apMax;
        this.side = side;
    }

    public UUID getId() { return id; }
    public UUID getCombatId() { return combatId; }
    public UUID getEntityId() { return entityId; }
    public int getInitiative() { return initiative; }
    public int getApCurrent() { return apCurrent; }
    public void setApCurrent(int v) { this.apCurrent = v; }
    public int getApMax() { return apMax; }
    public String getSide() { return side; }
}