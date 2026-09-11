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
    void nonNumericTableRowsReportErrorInsteadOfCrashing() {
        // Audit final: kaputte Zeilen (String min, fehlender value) dürfen keinen 500er werfen.
        var defs = List.of(Map.<String, Object>of(
            "name", "broken", "input", "mut",
            "table", List.of(
                Map.of("min", "x", "max", 10, "value", 5),
                Map.of("min", 11, "max", 20))));

        var result = service.evaluate(defs, Map.of("mut", 14));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().error()).contains("Invalid table row");
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

    @Test
    void overlappingTableRowsReportError() {
        // Audit P28: Ueberlappung darf nicht still first-match sein.
        var table = List.of(
            Map.<String, Object>of("min", 24, "max", 32, "value", 4),
            Map.<String, Object>of("min", 30, "max", 38, "value", 5));
        var defs = List.of(Map.<String, Object>of(
            "name", "sk", "input", "mut+klugheit", "table", table));

        var result = service.evaluate(defs, Map.of("mut", 15, "klugheit", 15));

        assertThat(result.getFirst().value()).isZero();
        assertThat(result.getFirst().error()).containsIgnoringCase("overlap");
    }

    @Test
    void nullFormulaDoesNotThrow() {
        var def = new java.util.HashMap<String, Object>();
        def.put("name", "x");
        def.put("formula", null);

        var result = service.evaluate(List.of(def), Map.of("mut", 5));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isZero();
    }

    @Test
    void nullTableInputReportsError() {
        var def = new java.util.HashMap<String, Object>();
        def.put("name", "x");
        def.put("input", null);
        def.put("table", List.of(Map.<String, Object>of("min", 1, "max", 2, "value", 1)));

        var result = service.evaluate(List.of(def), Map.of("mut", 5));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().error()).isNotNull();
    }
}
