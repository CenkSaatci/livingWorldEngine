package com.lwe.core.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DerivedValueServiceTest {

    private final DerivedValueService service = new DerivedValueService();

    @Test
    void evaluatesFormulaWithAttributes() {
        var defs = List.of(Map.<String, Object>of("name", "Schaden", "formula", "staerke*2"));

        var result = service.evaluate(defs, Map.of("staerke", 5));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Schaden");
        assertThat(result.getFirst().value()).isEqualTo(10.0);
    }

    @Test
    void returnsEmptyForNullInput() {
        assertThat(service.evaluate(null, Map.of())).isEmpty();
    }

    @Test
    void marksInvalidFormulaInsteadOfThrowing() {
        var defs = List.of(Map.<String, Object>of("name", "Kaputt", "formula", "staerke+"));

        var result = service.evaluate(defs, Map.of("staerke", 5));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).contains("Kaputt");
        assertThat(result.getFirst().value()).isZero();
    }

    @Test
    void evaluatesTableLookup() {
        // P28-T05: input-Ausdruck → Tabellen-Lookup (DSA: SK aus MU+KL+IN)
        var table = List.of(
            Map.<String, Object>of("min", 33, "max", 38, "value", 6),
            Map.<String, Object>of("min", 39, "max", 44, "value", 7));
        var defs = List.of(Map.<String, Object>of(
            "name", "sk", "input", "mut+klugheit+intuition", "table", table));

        var result = service.evaluate(defs, Map.of("mut", 14, "klugheit", 12, "intuition", 14));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEqualTo(7.0);
        assertThat(result.getFirst().error()).isNull();
    }

    @Test
    void tableGapReportsErrorWithReason() {
        var table = List.of(Map.<String, Object>of("min", 24, "max", 26, "value", 4));
        var defs = List.of(Map.<String, Object>of(
            "name", "sk", "input", "mut+klugheit+intuition", "table", table));

        var result = service.evaluate(defs, Map.of("mut", 14, "klugheit", 14, "intuition", 14));

        assertThat(result.getFirst().value()).isZero();
        assertThat(result.getFirst().error()).contains("42"); // 42 liegt in keiner Zeile
    }

    @Test
    void requiresTraitOmitsEntryWhenMissing() {
        var defs = List.of(Map.<String, Object>of(
            "name", "asp", "formula", "20+mut", "requiresTrait", "Zauberer"));

        var without = service.evaluate(defs, Map.of("mut", 12), List.of());
        assertThat(without).isEmpty();

        var with = service.evaluate(defs, Map.of("mut", 12), List.of("Zauberer II"));
        assertThat(with).hasSize(1);
        assertThat(with.getFirst().value()).isEqualTo(32.0);
    }
}
