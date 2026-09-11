package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_members", uniqueConstraints =
    @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
public class CampaignMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String role = "PLAYER";  // PLAYER | DM

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected CampaignMember() {}

    public CampaignMember(UUID campaignId, UUID userId, String role) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public Instant getJoinedAt() { return joinedAt; }
}
