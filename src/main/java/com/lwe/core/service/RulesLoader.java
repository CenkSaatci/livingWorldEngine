package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Zentrale Quelle für das Laden von Regelwerken aus einer Welt.
 * Behebt die Duplikation der world → gameSystem → rulesJson-Kette,
 * die vorher in ProbeService, CharacterSheetService, RestService und
 * CombatService identisch existierte.
 */
@Service
public class RulesLoader {

    private final WorldRepository worldRepo;
    private final GameSystemRepository systemRepo;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> RULES_MAP = new TypeReference<>() {};

    public RulesLoader(WorldRepository worldRepo, GameSystemRepository systemRepo,
                       ObjectMapper objectMapper) {
        this.worldRepo = worldRepo;
        this.systemRepo = systemRepo;
        this.objectMapper = objectMapper;
    }

    /** Liefert das GameSystem einer Welt oder null, wenn keins gesetzt/auffindbar ist. */
    public GameSystem loadSystem(World world) {
        if (world == null || world.getGameSystemId() == null) return null;
        return systemRepo.findById(world.getGameSystemId()).orElse(null);
    }

    /** Liefert das GameSystem zur worldId oder null. */
    public GameSystem loadSystem(UUID worldId) {
        if (worldId == null) return null;
        return loadSystem(worldRepo.findById(worldId).orElse(null));
    }

    /** Liefert die rulesJson als Map oder leere Map bei Fehler/fehlendem System. */
    public Map<String, Object> loadRules(World world) {
        var system = loadSystem(world);
        if (system == null || system.getRulesJson() == null || system.getRulesJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(system.getRulesJson(), RULES_MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Liefert die rulesJson zur worldId als Map oder leere Map. */
    public Map<String, Object> loadRules(UUID worldId) {
        return loadRules(worldRepo.findById(worldId).orElse(null));
    }
}
