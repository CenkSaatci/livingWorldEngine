package com.lwe.core.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleNamesTest {

    @Test
    void eqIsCaseInsensitiveAndNullSafe() {
        assertThat(RuleNames.eq("Athletik", "ATHLETIK")).isTrue();
        assertThat(RuleNames.eq("mut", "mut")).isTrue();
        assertThat(RuleNames.eq("mut", "kk")).isFalse();
        assertThat(RuleNames.eq(null, "mut")).isFalse();
        assertThat(RuleNames.eq("mut", null)).isFalse();
    }

    @Test
    void getFindsKeysIgnoringCase() {
        var map = Map.of("Mut", 14, "Koerperkraft", 12);
        assertThat(RuleNames.get(map, "mut")).isEqualTo(14);
        assertThat(RuleNames.get(map, "KOERPERKRAFT")).isEqualTo(12);
        assertThat(RuleNames.get(map, "fehlt")).isNull();
        assertThat(RuleNames.getOr(map, "fehlt", 10)).isEqualTo(10);
    }

    @Test
    void hasTraitIgnoresCaseAndTierSuffix() {
        assertThat(RuleNames.hasTrait(List.of("Zauberer II"), "zauberer")).isTrue();
        assertThat(RuleNames.hasTrait(List.of("Glück III"), "Glück")).isTrue();
        assertThat(RuleNames.hasTrait(List.of("Glückspilz"), "Glück")).isFalse();
        assertThat(RuleNames.hasTrait(List.of(), "Glück")).isFalse();
        assertThat(RuleNames.hasTrait(null, "Glück")).isFalse();
    }
}
