package com.lwe.core.service;

import com.lwe.core.domain.WorldMap;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorldMapService {

    private final WorldMapRepository mapRepo;
    private final WorldRepository worldRepo;

    public WorldMapService(WorldMapRepository mapRepo, WorldRepository worldRepo) {
        this.mapRepo = mapRepo;
        this.worldRepo = worldRepo;
    }

    public WorldMap getOrCreate(UUID worldId, UUID userId) {
        requireOwner(worldId, userId);
        return mapRepo.findByWorldId(worldId)
            .orElseGet(() -> mapRepo.save(new WorldMap(worldId)));
    }

    @Transactional
    public WorldMap update(UUID worldId, UUID userId, String imageUrl, Integer width, Integer height) {
        requireOwner(worldId, userId);
        var map = mapRepo.findByWorldId(worldId)
            .orElseGet(() -> new WorldMap(worldId));
        if (imageUrl != null) map.setImageUrl(imageUrl);
        if (width != null) map.setWidth(width);
        if (height != null) map.setHeight(height);
        return mapRepo.save(map);
    }

    /** F5-Audit: reiner Read fuer Owner und PUBLIC-Welten (erzeugt keine Map). */
    public WorldMap getMap(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        boolean canRead = world.getOwnerId().equals(userId)
            || "PUBLIC".equals(world.getVisibility());
        if (!canRead) {
            throw new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "Access denied");
        }
        return mapRepo.findByWorldId(worldId).orElse(null);
    }

    /** Nur der Owner darf die Map (an)legen — Fallback fuer GET ohne vorhandene Map. */
    public boolean canWrite(UUID worldId, UUID userId) {
        return worldRepo.findById(worldId)
            .map(w -> w.getOwnerId().equals(userId))
            .orElse(false);
    }

    public WorldMap getById(UUID mapId, UUID userId) {
        var map = mapRepo.findById(mapId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("MAP_NOT_FOUND", "Map not found"));
        requireOwner(map.getWorldId(), userId);
        return map;
    }

    private void requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)) {
            throw new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "Access denied");
        }
    }
}
