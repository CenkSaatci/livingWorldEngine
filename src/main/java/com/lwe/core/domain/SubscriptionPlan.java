package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "price_monthly_cents", nullable = false)
    private int priceMonthlyCents;

    @Column(name = "price_yearly_cents")
    private Integer priceYearlyCents;

    @Column(name = "max_worlds", nullable = false)
    private int maxWorlds = 1;

    @Column(name = "max_members_per_world", nullable = false)
    private int maxMembersPerWorld = 5;

    @Column(name = "max_entities_per_world", nullable = false)
    private int maxEntitiesPerWorld = 50;

    @Column(name = "max_storage_mb", nullable = false)
    private int maxStorageMb = 100;

    @Column(name = "ai_mode_allowed", nullable = false)
    private boolean aiModeAllowed;

    @Column(name = "features_json", nullable = false)
    private String featuresJson = "{}";

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SubscriptionPlan() {}

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public int getPriceMonthlyCents() { return priceMonthlyCents; }
    public void setPriceMonthlyCents(int v) { this.priceMonthlyCents = v; }
    public Integer getPriceYearlyCents() { return priceYearlyCents; }
    public void setPriceYearlyCents(Integer v) { this.priceYearlyCents = v; }
    public int getMaxWorlds() { return maxWorlds; }
    public void setMaxWorlds(int v) { this.maxWorlds = v; }
    public int getMaxMembersPerWorld() { return maxMembersPerWorld; }
    public void setMaxMembersPerWorld(int v) { this.maxMembersPerWorld = v; }
    public int getMaxEntitiesPerWorld() { return maxEntitiesPerWorld; }
    public void setMaxEntitiesPerWorld(int v) { this.maxEntitiesPerWorld = v; }
    public int getMaxStorageMb() { return maxStorageMb; }
    public void setMaxStorageMb(int v) { this.maxStorageMb = v; }
    public boolean isAiModeAllowed() { return aiModeAllowed; }
    public void setAiModeAllowed(boolean v) { this.aiModeAllowed = v; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String v) { this.featuresJson = v; }
    public int getPriority() { return priority; }
    public void setPriority(int v) { this.priority = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isUnlimited(int value) { return value == -1; }
    public boolean hasReachedLimit(int current, int max) {
        return !isUnlimited(max) && current >= max;
    }
}
