package com.lwe.rules;

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
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;

    public EngineResolver(RulesLoader rulesLoader, List<RuleEngine> engineList) {
        this.rulesLoader = rulesLoader;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            this.engines.put(engine.getDiceSystem(), engine);
        }
    }

    /** Engine des Systems; Fallback D20 (oder erste verfügbare Engine). */
    public RuleEngine resolve(World world, UUID campaignId) {
        var gs = rulesLoader.resolveSystem(campaignId, world);
        if (gs != null) {
            try {
                var engine = engines.get(DiceExpressionParser.detect(gs.getRulesJson()));
                if (engine != null) return engine;
            } catch (IllegalArgumentException ignored) {
                // unbekanntes Würfelsystem -> Fallback unten
            }
        }
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }
}
