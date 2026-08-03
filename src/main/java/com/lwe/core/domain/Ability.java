package com.lwe.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "abilities")
public class Ability {

    public enum AbilityType { ACTIVE, PASSIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_system_id", nullable = false)
    private UUID gameSystemId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "ability_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AbilityType type = AbilityType.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "effects_json", nullable = false)
    private String effectsJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stat_bonuses_json", nullable = false)
    private String statBonusesJson = "{}";

    @Column(name = "ap_cost", nullable = false)
    private int apCost = 1;

    @Column(name = "cooldown_rounds", nullable = false)
    private int cooldownRounds = 0;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType = "enemy";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Ability() {}

    public Ability(UUID gameSystemId, String name, AbilityType type) {
        this.gameSystemId = gameSystemId;
        this.name = name;
        this.type = type;
    }

    public UUID getId() { return id; }
    public UUID getGameSystemId() { return gameSystemId; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public AbilityType getType() { return type; }
    public void setType(AbilityType v) { this.type = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getEffectsJson() { return effectsJson; }
    public void setEffectsJson(String v) { this.effectsJson = v; }
    public String getStatBonusesJson() { return statBonusesJson; }
    public void setStatBonusesJson(String v) { this.statBonusesJson = v; }
    public int getApCost() { return apCost; }
    public void setApCost(int v) { this.apCost = v; }
    public int getCooldownRounds() { return cooldownRounds; }
    public void setCooldownRounds(int v) { this.cooldownRounds = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Instant getCreatedAt() { return createdAt; }
}
