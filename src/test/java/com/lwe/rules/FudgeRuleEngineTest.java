package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FudgeRuleEngineTest {

    private final FudgeRuleEngine engine = new FudgeRuleEngine();

    @Test
    void shouldRoll4dF() {
        var req = new RuleEngine.ProbeRequest("skill", 0, 0, 10, "4dF");
        var result = engine.executeProbe(req);

        assertThat(result.dice()).hasSize(4);
        assertThat(result.expression()).startsWith("4dF");
        // Jeder einzelne Wurf ist -1, 0 oder +1
        for (int roll : result.dice()) {
            assertThat(roll).isBetween(-1, 1);
        }
        // Summe ist zwischen -4 und +4
        assertThat(result.total()).isBetween(-4, 4);
    }

    @Test
    void shouldAcceptModifier() {
        var req = new RuleEngine.ProbeRequest("skill", 0, 3, 10, "4dF+1");
        var result = engine.executeProbe(req);

        assertThat(result.expression()).contains("+3"); // modifier from request (3)
        assertThat(result.total()).isBetween(-1, 7); // -4+3 bis 4+3
    }

    @Test
    void shouldUseProbeRequestDefaultWhenExpressionUnknown() {
        // Wenn diceExpression kein gültiger Fudge-Ausdruck ist, nutzt die Engine count=4
        var req = new RuleEngine.ProbeRequest("skill", 0, 0, 10, "");
        var result = engine.executeProbe(req);
        assertThat(result.dice()).hasSize(4);
    }
}
