package com.lwe.core.service;

import com.lwe.core.domain.Faction;
import com.lwe.core.domain.FactionRelation;
import com.lwe.core.repository.FactionRelationRepository;
import com.lwe.core.repository.FactionRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FactionService {

    private final FactionRepository factionRepo;
    private final FactionRelationRepository relationRepo;
    private final WorldAccess worldAccess;

    public FactionService(FactionRepository factionRepo,
                          FactionRelationRepository relationRepo,
                          WorldAccess worldAccess) {
        this.factionRepo = factionRepo;
        this.relationRepo = relationRepo;
        this.worldAccess = worldAccess;
    }

    // -- CRUD --

    @Transactional
    public Faction create(UUID worldId, UUID userId, String name, String description,
                          String color, UUID leaderEntityId) {
        requireOwner(worldId, userId);
        var faction = new Faction(worldId, name);
        if (description != null) faction.setDescription(description);
        if (color != null) faction.setColor(color);
        if (leaderEntityId != null) faction.setLeaderEntityId(leaderEntityId);
        return factionRepo.save(faction);
    }

    public List<Faction> list(UUID worldId, UUID userId) {
        requireOwner(worldId, userId);
        return factionRepo.findByWorldIdOrderByNameAsc(worldId);
    }

    public Faction getById(UUID factionId, UUID userId) {
        var faction = factionRepo.findById(factionId)
            .orElseThrow(() -> new FactionException("FACTION_NOT_FOUND", "Faction not found"));
        requireOwner(faction.getWorldId(), userId);
        return faction;
    }

    @Transactional
    public Faction update(UUID factionId, UUID userId, String name, String description,
                          String color, UUID leaderEntityId) {
        var faction = getById(factionId, userId);
        if (name != null) faction.setName(name);
        if (description != null) faction.setDescription(description);
        if (color != null) faction.setColor(color);
        if (leaderEntityId != null) faction.setLeaderEntityId(leaderEntityId);
        return factionRepo.save(faction);
    }

    @Transactional
    public void delete(UUID factionId, UUID userId) {
        var faction = getById(factionId, userId);
        factionRepo.delete(faction);
    }

    // -- Diplomatie --

    @Transactional
    public FactionRelation setRelation(UUID factionAId, UUID factionBId, String status, UUID userId) {
        var a = getById(factionAId, userId);
        var b = getById(factionBId, userId);
        if (!a.getWorldId().equals(b.getWorldId()))
            throw new FactionException("FACTION_WORLD_MISMATCH", "Factions must be in the same world");
        return saveRelation(factionAId, factionBId, status);
    }

    /**
     * System-interner Aufruf ohne User-Check (für KI-generierte Diplomatie-Änderungen).
     * Der Intent wurde bereits durch NpcIntentService validiert.
     */
    @Transactional
    public FactionRelation setRelationInternal(UUID factionAId, UUID factionBId, String status) {
        return saveRelation(factionAId, factionBId, status);
    }

    private FactionRelation saveRelation(UUID factionAId, UUID factionBId, String status) {
        var relation = relationRepo.findByFactionAIdAndFactionBId(factionAId, factionBId)
            .orElseGet(() -> relationRepo.findByFactionAIdAndFactionBId(factionBId, factionAId)
                .orElse(null));

        if (relation != null) {
            relation.setRelationStatus(status);
            return relationRepo.save(relation);
        }

        return relationRepo.save(new FactionRelation(factionAId, factionBId, status));
    }

    public List<FactionRelation> getRelations(UUID factionId) {
        return relationRepo.findByFactionAIdOrFactionBId(factionId, factionId);
    }

    // -- Ownership --

    private void requireOwner(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    public static class FactionException extends RuntimeException {
        private final String errorCode;
        public FactionException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
