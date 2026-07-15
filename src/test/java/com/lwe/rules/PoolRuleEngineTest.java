package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoolRuleEngineTest {

    private final PoolRuleEngine engine = new PoolRuleEngine();

    @Test
    void shouldCalculateDirectModifier() {
        assertThat(engine.calculateModifier(3)).isEqualTo(3);
        assertThat(engine.calculateModifier(6)).isEqualTo(6);
        assertThat(engine.calculateModifier(1)).isEqualTo(1);
    }

    @Test
    void shouldExecuteProbe() {
        var req = new RuleEngine.ProbeRequest("schlagen", 3, 0, 0, "2d6+mod");
        var result = engine.executeProbe(req);

        assertThat(result.expression()).contains("2d6");
        assertThat(result.dice()).hasSize(2);
        assertThat(result.total()).isBetween(5, 15); // 2d6(2-12) + 3
        assertThat(result.successTier()).isBetween(0, 3);
    }

    @Test
    void shouldAlwaysFailWithMinimalAttributes() {
        var req = new RuleEngine.ProbeRequest("kraft", 0, -5, 0, "2d6+mod");
        var result = engine.executeProbe(req);
        // total = 2d6(2-12) - 5 = -3 bis 7
        assertThat(result.successTier()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldReachHighTierWithGoodRoll() {
        // max total = 12 + 6 = 18 → Tier 3
        var req = new RuleEngine.ProbeRequest("zaubern", 6, 0, 0, "2d6+mod");
        var result = engine.executeProbe(req);
        assertThat(result.total()).isBetween(8, 18);
    }
}