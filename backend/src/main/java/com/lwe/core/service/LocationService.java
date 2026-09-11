package com.lwe.core.service;

import com.lwe.core.domain.Location;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService {

    private final LocationRepository repo;
    private final RegionRepository regionRepo;
    private final WorldAccess worldAccess;
    private final EntityEventService eventService;

    public LocationService(LocationRepository repo, RegionRepository regionRepo,
                           WorldAccess worldAccess, EntityEventService eventService) {
        this.repo = repo;
        this.regionRepo = regionRepo;
        this.worldAccess = worldAccess;
        this.eventService = eventService;
    }

    @Transactional
    public Location create(UUID regionId, UUID userId, String type, String name,
                           String description, String history, int population,
                           int wealth, String services, String factions,
                           boolean isCapital, String positionJson) {
        requireAccess(regionId, userId);
        var loc = new Location(regionId, type, name);
        if (description != null) loc.setDescription(description);
        if (history != null) loc.setHistory(history);
        loc.setPopulation(population);
        loc.setWealth(wealth);
        if (services != null) loc.setServices(services);
        if (factions != null) loc.setFactions(factions);
        loc.setCapital(isCapital);
        if (positionJson != null) loc.setPositionJson(positionJson);
        loc = repo.save(loc);
        eventService.publish("location", loc.getId(), "LOCATION_CREATED",
            "Ort " + name + " gegründet", null, 2, userId);
        return loc;
    }

    public List<Location> list(UUID regionId, UUID userId) {
        requireRead(regionId, userId); // T33-02
        return repo.findByRegionIdOrderByNameAsc(regionId);
    }

    public Location getById(UUID locationId, UUID userId) {
        var loc = repo.findById(locationId)
            .orElseThrow(() -> new LocationException("LOCATION_NOT_FOUND", "Location not found"));
        requireRead(loc.getRegionId(), userId); // T33-02
        return loc;
    }

    @Transactional
    public Location update(UUID locationId, UUID userId, String type, String name,
                           String description, String history, Integer population,
                           Integer wealth, String services, String factions,
                           Boolean isCapital, String positionJson) {
        var loc = getById(locationId, userId);
        if (type != null) loc.setType(type);
        if (name != null) loc.setName(name);
        if (description != null) loc.setDescription(description);
        if (history != null) loc.setHistory(history);
        if (population != null) loc.setPopulation(population);
        if (wealth != null) loc.setWealth(wealth);
        if (services != null) loc.setServices(services);
        if (factions != null) loc.setFactions(factions);
        if (isCapital != null) loc.setCapital(isCapital);
        if (positionJson != null) loc.setPositionJson(positionJson);
        return repo.save(loc);
    }

    @Transactional
    public void delete(UUID locationId, UUID userId) {
        var loc = getById(locationId, userId);
        repo.delete(loc);
    }

    @Transactional
    public void updatePosition(UUID locationId, UUID userId, String positionJson) {
        var loc = getById(locationId, userId);
        loc.setPositionJson(positionJson);
        repo.save(loc);
    }

    private void requireRead(UUID regionId, UUID userId) {
        var region = regionRepo.findById(regionId)
            .orElseThrow(() -> new LocationException("REGION_NOT_FOUND", "Region not found"));
        worldAccess.requireRead(region.getWorldId(), userId);
    }

    private void requireAccess(UUID regionId, UUID userId) {
        var region = regionRepo.findById(regionId)
            .orElseThrow(() -> new LocationException("REGION_NOT_FOUND", "Region not found"));
        worldAccess.requireAccess(region.getWorldId(), userId);
    }

    public static class LocationException extends RuntimeException {
        private final String errorCode;
        public LocationException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
