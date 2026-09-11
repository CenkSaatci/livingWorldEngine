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
}
