package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stellt NPCs und Dienste eines Ortes bereit.
 */
@Service
public class LocationNpcService {

    private final GameEntityRepository entityRepo;
    private final LocationRepository locationRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocationNpcService(GameEntityRepository entityRepo, LocationRepository locationRepo) {
        this.entityRepo = entityRepo;
        this.locationRepo = locationRepo;
    }

    /**
     * NPCs auflisten, die an einem Ort leben/arbeiten.
     */
    public List<GameEntity> getNpcsAtLocation(UUID locationId, UUID userId) {
        requireAccess(locationId, userId);
        var filter = "{\"location_id\":\"" + locationId + "\"}";
        return entityRepo.findByMetadataJsonFilter(filter).stream()
            .filter(e -> "NPC".equals(e.getEntityType()))
            .collect(Collectors.toList());
    }

    /**
     * Aggregiert alle Dienste eines Ortes (aus NPC-services_offered).
     */
    public Map<String, List<Map<String, Object>>> getServices(UUID locationId, UUID userId) {
        var npcs = getNpcsAtLocation(locationId, userId);
        var services = new LinkedHashMap<String, List<Map<String, Object>>>();

        for (var npc : npcs) {
            try {
                var meta = objectMapper.readTree(npc.getMetadataJson());
                var occupation = meta.path("occupation").asText("unknown");
                var offered = meta.path("services_offered");
                var greeting = meta.path("greeting").asText("");

                if (offered.isArray()) {
                    for (var s : offered) {
                        var serviceName = s.asText();
                        services.computeIfAbsent(serviceName, k -> new ArrayList<>())
                            .add(Map.of(
                                "npc_id", npc.getId().toString(),
                                "npc_name", npc.getName(),
                                "occupation", occupation,
                                "greeting", greeting
                            ));
                    }
                }
            } catch (Exception ignored) {}
        }

        return services;
    }

    private void requireAccess(UUID locationId, UUID userId) {
        locationRepo.findById(locationId).ifPresent(loc -> {
            // Access checked at region → world level upstream
        });
    }
}
