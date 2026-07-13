package com.lwe.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.rules.DiceExpressionParser;
import com.lwe.rules.RuleEngine;
import static com.lwe.core.service.WorldEventService.EventType.*;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RollService {

    private final GameEntityRepository entityRepo;
    private final WorldRepository worldRepo;
    private final GameSystemRepository gameSystemRepo;
    private final WorldEventService eventService;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RollService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                       GameSystemRepository gameSystemRepo, WorldEventService eventService,
                       java.util.List<RuleEngine> engineList) {
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.eventService = eventService;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            var system = engine.getClass().getSimpleName().toLowerCase().contains("pool")
                ? DiceExpressionParser.DiceSystem.POOL
                : DiceExpressionParser.DiceSystem.D20;
            this.engines.put(system, engine);
        }
    }

    public RollResult executeRoll(UUID userId, UUID worldId, UUID entityId,
                                  String skillId, int modifier, int target) {
        // 1. Entity prüfen
        var entity = entityRepo.findById(entityId).orElse(null);
        if (entity == null) {
            return error(skillId, target, "Entity not found");
        }

        // 2. Welt + Zugriff prüfen
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null) {
            return error(skillId, target, "World not found");
        }
        if (!world.getOwnerId().equals(userId)) {
            return error(skillId, target, "Access denied");
        }

        // 3. Attributswert extrahieren
        var attrValue = extractAttribute(entity, skillId).orElse(10);

        // 4. Game-System laden → Engine bestimmen
        var engine = getEngineForWorld(world);
        var req = new RuleEngine.ProbeRequest(skillId, attrValue, modifier, target);
        var probeResult = engine.executeProbe(req);

        // 5. Event loggen
        eventService.publish(worldId, PROBE_ROLLED, entityId, null, Map.of(
            "skillId", skillId,
            "total", probeResult.total(),
            "target", target,
            "success", probeResult.success()
        ));

        // 6. Ergebnis
        return new RollResult(skillId, probeResult.expression(), probeResult.dice(),
            probeResult.total(), target, probeResult.success(), null);
    }

    private RuleEngine getEngineForWorld(com.lwe.core.domain.World world) {
        if (world.getGameSystemId() != null) {
            var gs = gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
            if (gs != null) {
                var system = DiceExpressionParser.detect(gs.getRulesJson());
                var engine = engines.get(system);
                if (engine != null) return engine;
            }
        }
        // Fallback auf D20
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }

    private Optional<Integer> extractAttribute(GameEntity entity, String skillName) {
        try {
            var tree = objectMapper.readTree(entity.getAttributesJson());
            var node = tree.get(skillName);
            if (node != null && node.isInt()) return Optional.of(node.asInt());
        } catch (JsonProcessingException ignored) {}
        return Optional.empty();
    }

    private RollResult error(String skillId, int target, String msg) {
        return new RollResult(skillId, "?", new int[]{0}, 0, target, false, msg);
    }

    public record RollResult(String skillId, String expression, int[] dice, int total,
                             int target, boolean success, String error) {}
}