package com.lwe.core.util;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.GameEntityRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Runde 1 (Audit F1/F3): Kontrolle über einen Charakter/NPC.
 *
 * <p>Regeln:
 * <ul>
 *   <li>Owner (owner_user_id == user) darf steuern (plus Welt-Zugriff).</li>
 *   <li>DM (Welt-Owner oder DM-Mitglied) darf jeden Charakter steuern.</li>
 *   <li>owner_user_id == null (Legacy/NPC ohne Zuordnung) → member-level
 *       ({@link WorldAccess#requireAccess}), damit Altbestand spielbar bleibt.</li>
 * </ul>
 */
@Component
public class EntityAccess {

    private final GameEntityRepository entityRepo;
    private final WorldAccess worldAccess;

    public EntityAccess(GameEntityRepository entityRepo, WorldAccess worldAccess) {
        this.entityRepo = entityRepo;
        this.worldAccess = worldAccess;
    }

    /** Lädt die Entity und prüft Kontrolle (404 wenn nicht vorhanden). */
    public GameEntity requireControl(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("ENTITY_NOT_FOUND", "Entity not found"));
        checkControl(entity, userId);
        return entity;
    }

    /** Wie {@link #requireControl(UUID, UUID)}, zusätzlich Welt-Zugehörigkeit prüfen. */
    public GameEntity requireControl(UUID entityId, UUID userId, UUID expectedWorldId) {
        var entity = requireControl(entityId, userId);
        if (expectedWorldId != null && !entity.getWorldId().equals(expectedWorldId)) {
            throw new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "Entity is not in this world");
        }
        return entity;
    }

    /** Prüft Kontrolle für eine bereits geladene Entity (kein Repo-Zugriff). */
    public void checkControl(GameEntity entity, UUID userId) {
        var owner = entity.getOwnerUserId();
        if (owner == null) {
            worldAccess.requireAccess(entity.getWorldId(), userId);
            return;
        }
        if (owner.equals(userId)) {
            worldAccess.requireAccess(entity.getWorldId(), userId);
            return;
        }
        worldAccess.requireDm(entity.getWorldId(), userId);
    }
}
