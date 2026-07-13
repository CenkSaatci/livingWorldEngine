package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldReturnUnknownForInvalid() {
        assertThat(DiceExpressionParser.detect("not json")).isEqualTo(DiceExpressionParser.DiceSystem.UNKNOWN);
        assertThat(DiceExpressionParser.detect("{}")).isEqualTo(DiceExpressionParser.DiceSystem.UNKNOWN);
    }
}