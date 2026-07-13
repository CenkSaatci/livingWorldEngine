package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🔴 RED: Test für die Rule-Engine. Erwartet, dass D20Lite- und Pool-System
 * korrekt Proben ausführen.
 */
class D20RuleEngineTest {

    private final D20RuleEngine engine = new D20RuleEngine();

    @Test
    void shouldCalculateModifier() {
        assertThat(engine.calculateModifier(10)).isZero();
        assertThat(engine.calculateModifier(14)).isEqualTo(2);
        assertThat(engine.calculateModifier(18)).isEqualTo(4);
        assertThat(engine.calculateModifier(8)).isEqualTo(-1);
        assertThat(engine.calculateModifier(1)).isEqualTo(-5);
    }

    @Test
    void shouldExecuteProbe() {
        var req = new RuleEngine.ProbeRequest("athletik", 16, 0, 15);
        var result = engine.executeProbe(req);

        assertThat(result.expression()).isEqualTo("1d20+3");
        assertThat(result.total()).isBetween(4, 23);
        assertThat(result.dice()).hasSize(1);
        assertThat(result.dice()[0]).isBetween(1, 20);
    }

    @Test
    void shouldSucceedWithEasyTarget() {
        var req = new RuleEngine.ProbeRequest("staerke", 18, 2, 5);
        var result = engine.executeProbe(req);
        // min total = 1 (roll) + 4 (mod) + 2 (bonus) = 7 > 5
        assertThat(result.success()).isTrue();
        assertThat(result.total()).isGreaterThan(5);
    }

    @Test
    void shouldFailWithImpossibleTarget() {
        var req = new RuleEngine.ProbeRequest("kraft", 1, 0, 50);
        var result = engine.executeProbe(req);

        assertThat(result.success()).isFalse();
    }
}