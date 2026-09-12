package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1: Die Beispielsysteme sind reine Daten — sie müssen gegen das echte
 * DEFAULT_SCHEMA validieren (Generik-Nachweis, keine Sonderlogik im Code).
 */
class ExamplesSchemaValidationTest {

    private final RuleSchemaValidator validator = new RuleSchemaValidator(new ObjectMapper());

    @Test
    void exampleSystemsValidateAgainstDefaultSchema() throws Exception {
        for (String file : List.of("dnd5e.json", "coc7e.json", "dsa5.json", "dsa5-playtest.json")) {
            // Audit T7: komplett validieren (kein Key-Filter) — sonst rutschen
            // neue/veraltete Felder ungeprueft durch.
            var raw = Files.readString(Path.of("../docs/examples/" + file));
            var errors = validator.validate(raw, RuleSchemaValidator.DEFAULT_SCHEMA);
            assertThat(errors).as(file + " muss schema-konform sein: " + errors).isEmpty();
        }
    }
}
