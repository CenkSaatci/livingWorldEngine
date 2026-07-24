package com.lwe.core.service;

import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

@Service
public class WorldService {

    private final WorldRepository worldRepo;
    private final WorldMemberRepository memberRepo;
    private final GameSystemRepository gameSystemRepo;
    private final QuotaService quotaService;
    private final RegionRepository regionRepo;
    private final LocationRepository locationRepo;
    private final GameEntityRepository entityRepo;
    private final FactionRepository factionRepo;
    private final FactionRelationRepository factionRelationRepo;
    private final WorldMapRepository worldMapRepo;
    private final RegionWeatherRepository regionWeatherRepo;

    public WorldService(WorldRepository worldRepo, WorldMemberRepository memberRepo,
                        GameSystemRepository gameSystemRepo, QuotaService quotaService,
                        RegionRepository regionRepo, LocationRepository locationRepo,
                        GameEntityRepository entityRepo, FactionRepository factionRepo,
                        FactionRelationRepository factionRelationRepo,
                        WorldMapRepository worldMapRepo,
                        RegionWeatherRepository regionWeatherRepo) {
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.quotaService = quotaService;
        this.regionRepo = regionRepo;
        this.locationRepo = locationRepo;
        this.entityRepo = entityRepo;
        this.factionRepo = factionRepo;
        this.factionRelationRepo = factionRelationRepo;
        this.worldMapRepo = worldMapRepo;
        this.regionWeatherRepo = regionWeatherRepo;
    }

    @Transactional
    public World create(String name, UUID ownerId, UUID gameSystemId, String settingsJson, User user) {
        quotaService.checkCanCreateWorld(ownerId, user);
        if (gameSystemId != null) {
            gameSystemRepo.findById(gameSystemId)
                .filter(GameSystem::isActive)
                .orElseThrow(() -> new WorldException("WORLD_GAME_SYSTEM_INACTIVE",
                    "Game system not found or inactive"));
        }
        var world = new World(name, ownerId, gameSystemId, settingsJson);
        return worldRepo.save(world);
    }

    /**
     * Welten, die der User besitzt.
     */
    public List<World> listOwned(UUID userId) {
        return worldRepo.findByOwnerIdAndActiveTrue(userId);
    }

    /**
     * Welten, auf die der User Zugriff hat (Owner + Member).
     */
    public List<World> listAccessible(UUID userId) {
        var memberWorldIds = memberRepo.findWorldIdsByUserId(userId);
        if (memberWorldIds.isEmpty()) {
            return worldRepo.findByOwnerIdAndActiveTrue(userId);
        }
        var owned = worldRepo.findByOwnerIdAndActiveTrue(userId);
        var joined = worldRepo.findAllById(memberWorldIds).stream()
            .filter(World::isActive)
            .filter(w -> !w.getOwnerId().equals(userId)) // nicht doppelt
            .toList();
        return java.util.stream.Stream.concat(owned.stream(), joined.stream()).toList();
    }

    public AccessibleResult listAccessible(UUID userId, int page, int size) {
        var pg = worldRepo.findAccessibleByUserId(userId, PageRequest.of(page, size));
        return new AccessibleResult(pg.getContent(), (int) pg.getTotalElements(), page, pg.hasNext());
    }

    public record AccessibleResult(List<World> items, int total, int page, boolean hasMore) {}

    /**
     * Welt-Detail — Owner und Mitglieder haben Zugriff.
     */
    public World getById(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId) && !memberRepo.existsByWorldIdAndUserId(worldId, userId)) {
            throw new WorldException("WORLD_ACCESS_DENIED", "Access denied");
        }
        return world;
    }

    @Transactional
    public World update(UUID worldId, UUID userId, String name, String settingsJson, UUID gameSystemId) {
        var world = getById(worldId, userId);
        if (!world.getOwnerId().equals(userId)) {
            throw new WorldException("WORLD_OWNER_REQUIRED", "Only the owner may update this world");
        }
        if (name != null) world.setName(name);
        if (settingsJson != null) world.setSettingsJson(settingsJson);
        if (gameSystemId != null) {
            gameSystemRepo.findById(gameSystemId)
                .filter(GameSystem::isActive)
                .orElseThrow(() -> new WorldException("WORLD_GAME_SYSTEM_INACTIVE",
                    "Game system not found or inactive"));
            world.setGameSystemId(gameSystemId);
        }
        return worldRepo.save(world);
    }

    @Transactional
    public World clone(UUID worldId, UUID userId, User user) {
        var original = requireOwner(worldId, userId);
        quotaService.checkCanCreateWorld(userId, user);

        var clone = new World(original.getName() + " (Copy)", userId,
            original.getGameSystemId(), original.getSettingsJson());
        clone.setCurrentGameTime(original.getCurrentGameTime());
        clone = worldRepo.save(clone);

        // Regions
        var regionIdMap = new HashMap<UUID, UUID>();
        for (var r : regionRepo.findByWorldIdOrderByNameAsc(worldId)) {
            var copy = new Region(clone.getId(), r.getName());
            copy.setDescription(r.getDescription());
            copy.setHistory(r.getHistory());
            copy.setDangerLevel(r.getDangerLevel());
            copy.setClimate(r.getClimate());
            copy.setResources(r.getResources());
            copy.setFactions(r.getFactions());
            copy.setPopulation(r.getPopulation());
            copy.setPositionJson(r.getPositionJson());
            copy.setPolygonPoints(r.getPolygonPoints());
            copy.setCapitalId(r.getCapitalId()); // keep original, will remap after locations
            var saved = regionRepo.save(copy);
            regionIdMap.put(r.getId(), saved.getId());
        }

        // Locations (using original region IDs)
        var locationIdMap = new HashMap<UUID, UUID>();
        var origRegionIds = regionIdMap.keySet().stream().toList();
        for (var loc : locationRepo.findByRegionIdIn(origRegionIds)) {
            var newRegionId = regionIdMap.get(loc.getRegionId());
            if (newRegionId == null) continue;
            var copy = new Location(newRegionId, loc.getType(), loc.getName());
            copy.setName(loc.getName());
            copy.setDescription(loc.getDescription());
            copy.setHistory(loc.getHistory());
            copy.setPopulation(loc.getPopulation());
            copy.setWealth(loc.getWealth());
            copy.setServices(loc.getServices());
            copy.setFactions(loc.getFactions());
            copy.setPositionJson(loc.getPositionJson());
            var saved = locationRepo.save(copy);
            locationIdMap.put(loc.getId(), saved.getId());
        }

        // Remap region capital_id to cloned location IDs
        for (var entry : regionIdMap.entrySet()) {
            var originalRegion = regionRepo.findById(entry.getKey()).orElse(null);
            if (originalRegion != null && originalRegion.getCapitalId() != null) {
                var newCapitalId = locationIdMap.get(originalRegion.getCapitalId());
                if (newCapitalId != null) {
                    var clonedRegion = regionRepo.findById(entry.getValue()).orElse(null);
                    if (clonedRegion != null) {
                        clonedRegion.setCapitalId(newCapitalId);
                        regionRepo.save(clonedRegion);
                    }
                }
            }
        }

        // Factions
        var factionIdMap = new HashMap<UUID, UUID>();
        for (var f : factionRepo.findByWorldIdOrderByNameAsc(worldId)) {
            var copy = new Faction(clone.getId(), f.getName());
            copy.setDescription(f.getDescription());
            copy.setColor(f.getColor());
            var saved = factionRepo.save(copy);
            factionIdMap.put(f.getId(), saved.getId());
        }

        // Faction relations
        var origFactionIds = factionIdMap.keySet().stream().toList();
        for (var fr : factionRelationRepo.findByFactionAIdIn(origFactionIds)) {
            var newA = factionIdMap.get(fr.getFactionAId());
            var newB = factionIdMap.get(fr.getFactionBId());
            if (newA != null && newB != null) {
                var copy = new FactionRelation(newA, newB, fr.getRelationStatus());
                factionRelationRepo.save(copy);
            }
        }

        // Entities
        for (var e : entityRepo.findByWorldIdAndActiveTrue(worldId)) {
            var newFactionId = e.getFactionId() != null ? factionIdMap.get(e.getFactionId()) : null;
            var copy = new GameEntity(clone.getId(), e.getEntityType(), e.getName());
            copy.setAttributesJson(e.getAttributesJson());
            copy.setInventoryJson(e.getInventoryJson());
            copy.setPositionJson(e.getPositionJson());
            copy.setMetadataJson(e.getMetadataJson());
            copy.setFactionId(newFactionId);
            copy.setBackstory(e.getBackstory());
            copy.setAge(e.getAge());
            copy.setExperienceLevel(e.getExperienceLevel());
            copy.setSocialStanding(e.getSocialStanding());
            entityRepo.save(copy);
        }

        // World maps
        var finalCloneId = clone.getId();
        worldMapRepo.findByWorldId(worldId).ifPresent(m -> {
            var copy = new WorldMap(finalCloneId);
            copy.setName(m.getName());
            copy.setImageUrl(m.getImageUrl());
            copy.setWidth(m.getWidth());
            copy.setHeight(m.getHeight());
            worldMapRepo.save(copy);
        });

        // Region weather
        for (var rw : regionWeatherRepo.findByRegionIdIn(origRegionIds)) {
            var newRegionId = regionIdMap.get(rw.getRegionId());
            if (newRegionId != null) {
                var copy = new RegionWeather(newRegionId);
                copy.setTemperature(rw.getTemperature());
                copy.setWind(rw.getWind());
                copy.setWeatherType(rw.getWeatherType());
                regionWeatherRepo.save(copy);
            }
        }

        return clone;
    }

    @Transactional
    public void delete(UUID worldId, UUID userId) {
        var world = requireOwner(worldId, userId);
        world.setActive(false);
        worldRepo.save(world);
    }

    @Transactional
    public WorldMember addMember(UUID worldId, UUID userId, UUID memberUserId, String role) {
        var world = requireOwner(worldId, userId); // nur Owner darf einladen
        quotaService.checkCanAddMember(worldId, world.getOwnerId());
        if (memberRepo.existsByWorldIdAndUserId(worldId, memberUserId)) {
            throw new WorldException("WORLD_MEMBER_ALREADY", "User is already a member");
        }
        var member = new WorldMember(worldId, memberUserId, role);
        return memberRepo.save(member);
    }

    @Transactional
    public void removeMember(UUID worldId, UUID userId, UUID memberUserId) {
        requireOwner(worldId, userId);
        var member = memberRepo.findByWorldIdAndUserId(worldId, memberUserId)
            .orElseThrow(() -> new WorldException("ENTITY_NOT_FOUND", "Member not found"));
        memberRepo.delete(member);
    }

    public List<WorldMember> listMembers(UUID worldId, UUID userId) {
        requireOwner(worldId, userId);
        return memberRepo.findByWorldId(worldId);
    }

    private World requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)) {
            throw new WorldException("WORLD_OWNER_REQUIRED", "Only the owner may perform this action");
        }
        return world;
    }

    public static class WorldException extends RuntimeException {
        private final String errorCode;
        public WorldException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}