package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiceExpressionTest {

    @Test
    void shouldParse2d6Plus3() {
        var expr = new DiceExpression("2d6+3");

        assertThat(expr.getCount()).isEqualTo(2);
        assertThat(expr.getSides()).isEqualTo(6);
        assertThat(expr.getModifier()).isEqualTo(3);
    }

    @Test
    void shouldParse1d20() {
        var expr = new DiceExpression("1d20");

        assertThat(expr.getCount()).isEqualTo(1);
        assertThat(expr.getSides()).isEqualTo(20);
        assertThat(expr.getModifier()).isZero();
    }

    @Test
    void shouldParse3d8Minus2() {
        var expr = new DiceExpression("3d8-2");

        assertThat(expr.getCount()).isEqualTo(3);
        assertThat(expr.getSides()).isEqualTo(8);
        assertThat(expr.getModifier()).isEqualTo(-2);
    }

    @Test
    void shouldReturnCorrectRollCount() {
        var expr = new DiceExpression("5d4");

        assertThat(expr.getRolls()).hasSize(5);
    }

    @Test
    void totalShouldEqualSumOfRollsPlusModifier() {
        var expr = new DiceExpression("3d6+2");

        var rolls = expr.getRolls();
        assertThat(rolls).hasSize(3);
        var sum = rolls[0] + rolls[1] + rolls[2];
        assertThat(expr.getTotal()).isEqualTo(sum + 2);
    }

    @Test
    void shouldThrowForInvalidExpression() {
        assertThatThrownBy(() -> new DiceExpression("abc"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiceExpression("2d6+3+1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiceExpression(""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
