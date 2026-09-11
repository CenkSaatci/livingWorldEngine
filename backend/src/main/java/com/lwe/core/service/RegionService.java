package com.lwe.core.service;

import com.lwe.core.domain.Region;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RegionService {

    private final RegionRepository repo;
    private final WorldAccess worldAccess;
    private final EntityEventService eventService;

    public RegionService(RegionRepository repo, WorldAccess worldAccess,
                         EntityEventService eventService) {
        this.repo = repo;
        this.worldAccess = worldAccess;
        this.eventService = eventService;
    }

    @Transactional
    public Region create(UUID worldId, UUID userId, String name, String description,
                         String history, int dangerLevel, String climate,
                         String resources, String factions, String positionJson,
                         String polygonPoints) {
        requireOwner(worldId, userId);

        var region = new Region(worldId, name);
        if (description != null) region.setDescription(description);
        if (history != null) region.setHistory(history);
        region.setDangerLevel(dangerLevel);
        if (climate != null) region.setClimate(climate);
        if (resources != null) region.setResources(resources);
        if (factions != null) region.setFactions(factions);
        if (positionJson != null) region.setPositionJson(positionJson);
        if (polygonPoints != null) region.setPolygonPoints(polygonPoints);

        region = repo.save(region);

        eventService.publish("region", region.getId(), "REGION_CREATED",
            "Region " + name + " gegründet", null, 2, userId);
        return region;
    }

    public List<Region> list(UUID worldId, UUID userId) {
        worldAccess.requireRead(worldId, userId); // T33-02
        return repo.findByWorldIdOrderByNameAsc(worldId);
    }

    public Region getById(UUID regionId, UUID userId) {
        var region = repo.findById(regionId)
            .orElseThrow(() -> new RegionException("REGION_NOT_FOUND", "Region not found"));
        worldAccess.requireRead(region.getWorldId(), userId); // T33-02
        return region;
    }

    @Transactional
    public Region update(UUID regionId, UUID userId, String name, String description,
                         String history, Integer dangerLevel, String climate,
                         String resources, String factions, Integer population,
                         String positionJson, String polygonPoints) {
        var region = getById(regionId, userId);
        if (name != null) region.setName(name);
        if (description != null) region.setDescription(description);
        if (history != null) region.setHistory(history);
        if (dangerLevel != null) region.setDangerLevel(dangerLevel);
        if (climate != null) region.setClimate(climate);
        if (resources != null) region.setResources(resources);
        if (factions != null) region.setFactions(factions);
        if (population != null) region.setPopulation(population);
        if (positionJson != null) region.setPositionJson(positionJson);
        if (polygonPoints != null) region.setPolygonPoints(polygonPoints);
        return repo.save(region);
    }

    @Transactional
    public void delete(UUID regionId, UUID userId) {
        var region = getById(regionId, userId);
        repo.delete(region);
    }

    private void requireOwner(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    public static class RegionException extends RuntimeException {
        private final String errorCode;
        public RegionException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
