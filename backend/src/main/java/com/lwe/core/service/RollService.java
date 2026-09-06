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
    private final RulesLoader rulesLoader;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper;

    public RollService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                       GameSystemRepository gameSystemRepo, WorldEventService eventService,
                       java.util.List<RuleEngine> engineList,
                       RulesLoader rulesLoader,
                       ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.eventService = eventService;
        this.rulesLoader = rulesLoader;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            this.engines.put(engine.getDiceSystem(), engine);
        }
    }

    public RollResult executeRoll(UUID userId, UUID worldId, UUID entityId,
                                  String skillId, int modifier, int target) {
        return executeRoll(userId, worldId, entityId, skillId, modifier, target, null);
    }

    public RollResult executeRoll(UUID userId, UUID worldId, UUID entityId,
                                  String skillId, int modifier, int target, UUID campaignId) {
        var entity = entityRepo.findById(entityId).orElse(null);
        if (entity == null) return error(skillId, target, "ENTITY_NOT_FOUND");

        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null) return error(skillId, target, "WORLD_NOT_FOUND");
        if (!world.getOwnerId().equals(userId)) {
            return error(skillId, target, "Access denied");
        }

        // 3. Attributswert extrahieren
        var attrValue = AttributeUtils.extractAttribute(entity, skillId).orElse(10);

        // 4. Game-System laden → Engine bestimmen
        var engine = getEngineForWorld(world, campaignId);
        var diceExpr = resolveDiceExpression(world, campaignId);
        var req = new RuleEngine.ProbeRequest(skillId, attrValue, modifier, target, diceExpr);
        var probeResult = engine.executeProbe(req);

        // 5. Event loggen
        eventService.publish(worldId, campaignId, PROBE_ROLLED, entityId, null, Map.of(
            "skillId", skillId,
            "total", probeResult.total(),
            "target", target,
            "success", probeResult.success()
        ));

        // 6. Ergebnis
        return new RollResult(skillId, probeResult.expression(), probeResult.dice(),
            probeResult.total(), target, probeResult.success(), null);
    }

    private RuleEngine getEngineForWorld(com.lwe.core.domain.World world, UUID campaignId) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs != null) {
            try {
                var system = DiceExpressionParser.detect(gs.getRulesJson());
                var engine = engines.get(system);
                if (engine != null) return engine;
            } catch (IllegalArgumentException e) {
                // unsupported dice system → fall through to fallback
            }
        }
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }

    private String resolveDiceExpression(com.lwe.core.domain.World world, UUID campaignId) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs != null) {
            try {
                var tree = objectMapper.readTree(gs.getRulesJson());
                return tree.path("dice_mechanics").path("probe").asText("1d20+mod");
            } catch (Exception e) {
                // fall through
            }
        }
        return "1d20+mod";
    }

    private RollResult error(String skillId, int target, String msg) {
        return new RollResult(skillId, "?", new int[]{0}, 0, target, false, msg);
    }

    public record RollResult(String skillId, String expression, int[] dice, int total,
                             int target, boolean success, String error) {}
}