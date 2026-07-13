package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "world_members", uniqueConstraints =
    @UniqueConstraint(columnNames = {"world_id", "user_id"}))
public class WorldMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String role = "PLAYER";  // PLAYER | DM

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected WorldMember() {}

    public WorldMember(UUID worldId, UUID userId, String role) {
        this.worldId = worldId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getId() { return id; }
    public UUID getWorldId() { return worldId; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}