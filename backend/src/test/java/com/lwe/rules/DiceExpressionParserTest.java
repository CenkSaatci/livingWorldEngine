package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiceExpressionParserTest {

    @Test
    void shouldDetectD20() {
        var rules = """
            {"version":1,"attributes":[],"dice_mechanics":{"probe":"1d20+mod"}}
            """;
        assertThat(DiceExpressionParser.detect(rules)).isEqualTo(DiceExpressionParser.DiceSystem.D20);
    }

    @Test
    void shouldDetectPool() {
        var rules = """
            {"version":1,"attributes":[],"dice_mechanics":{"probe":"2d6+mod"}}
            """;
        assertThat(DiceExpressionParser.detect(rules)).isEqualTo(DiceExpressionParser.DiceSystem.POOL);
    }

    @Test
    void shouldThrowForInvalidJson() {
        assertThatThrownBy(() -> DiceExpressionParser.detect("not json"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowForMissingProbe() {
        assertThatThrownBy(() -> DiceExpressionParser.detect("{}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDetectThrown() {
        var rules = """
            {"version":1,"attributes":[],"dice_mechanics":{"probe":"1d100"}}
            """;
        assertThat(DiceExpressionParser.detect(rules)).isEqualTo(DiceExpressionParser.DiceSystem.THROWN);
    }

    @Test
    void shouldParseProbe() {
        var result = DiceExpressionParser.parseProbe("3d6+mod");
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.sides()).isEqualTo(6);
    }
}