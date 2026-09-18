package com.lwe.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DamageExpressionTest {

    @Test
    void parsesDiceFlatAndAttribute() {
        var p = DamageExpression.parse("1d8+staerke+2");

        assertThat(p.dice()).isEqualTo("1d8");
        assertThat(p.attr()).isEqualTo("staerke");
        assertThat(p.flat()).isEqualTo(2);
        assertThat(p.attrBonusSign()).isEqualTo(1);
    }

    @Test
    void parsesSpacesAndUmlauts() {
        var p = DamageExpression.parse(" 1d6 + stärke + 1 ");

        assertThat(p.dice()).isEqualTo("1d6");
        assertThat(p.attr()).isEqualTo("stärke");
        assertThat(p.flat()).isEqualTo(1);
    }

    @Test
    void keepsNegativeAttributeSign() {
        var p = DamageExpression.parse("1d6-staerke");

        assertThat(p.attr()).isEqualTo("staerke");
        assertThat(p.attrBonusSign()).isEqualTo(-1);
    }

    @Test
    void sumsMultipleFlatTerms() {
        var p = DamageExpression.parse("2d6+2-1");

        assertThat(p.dice()).isEqualTo("2d6");
        assertThat(p.flat()).isEqualTo(1);
        assertThat(p.attr()).isNull();
    }

    @Test
    void rejectsInvalidExpressions() {
        assertThat(DamageExpression.parse("1d6+staerke+mut")).isNull();
        assertThat(DamageExpression.parse("1d6+")).isNull();
        assertThat(DamageExpression.parse("6")).isNull();
        assertThat(DamageExpression.parse("1d6++2")).isNull();
    }
}
