package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "world_invites")
public class WorldInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WorldInvite() {}

    public WorldInvite(UUID worldId, UUID createdBy, String token, int maxUses) {
        this.worldId = worldId;
        this.createdBy = createdBy;
        this.token = token;
        this.maxUses = maxUses;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public String getToken() { return token; }
    public UUID getCreatedBy() { return createdBy; }
    public int getMaxUses() { return maxUses; }
    public int getUseCount() { return useCount; }
    public void setUseCount(int v) { this.useCount = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
