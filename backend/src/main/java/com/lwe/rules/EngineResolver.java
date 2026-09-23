package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.World;
import com.lwe.core.service.RulesLoader;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bestimmt die Rule-Engine zum Kampagnen-/Welt-System.
 * ADR-014: eine Stelle statt identischer Kopien in CombatService und RollService.
 */
@Component
public class EngineResolver {

    private final RulesLoader rulesLoader;
    private final ObjectMapper objectMapper;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;

    public EngineResolver(RulesLoader rulesLoader, List<RuleEngine> engineList,
                          ObjectMapper objectMapper) {
        this.rulesLoader = rulesLoader;
        this.objectMapper = objectMapper;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            this.engines.put(engine.getDiceSystem(), engine);
        }
    }

    /**
     * Engine des Systems. Fehlendes System oder fehlendes/leeres {@code probe} =
     * dokumentierter D20-Default; ein <b>vorhandenes, aber ungültiges</b> {@code probe}
     * ist ein Fehler (A4, fail-closed) statt eines stillen D20-Fallbacks.
     */
    public RuleEngine resolve(World world, UUID campaignId) {
        var gs = rulesLoader.resolveSystem(campaignId, world);
        if (gs == null) return d20();
        String probe;
        try {
            probe = objectMapper.readTree(gs.getRulesJson())
                .path("dice_mechanics").path("probe").asText("");
        } catch (Exception e) {
            throw new IllegalStateException("Regel-JSON nicht lesbar: " + e.getMessage(), e);
        }
        if (probe.isBlank()) return d20();
        DiceExpressionParser.DiceSystem system;
        try {
            system = DiceExpressionParser.detect(gs.getRulesJson());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Ungültiges Würfelsystem '" + probe + "'", e);
        }
        var engine = engines.get(system);
        if (engine == null) {
            throw new IllegalStateException("Keine Engine für Würfelsystem " + system);
        }
        return engine;
    }

    private RuleEngine d20() {
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }
}
