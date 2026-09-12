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

    private static final List<String> RULES_KEYS = List.of(
        "version", "probeType", "progressionType", "modifierFormula", "features",
        "derived_values", "abilities", "progression", "magic", "psionics",
        "conditionals", "attributes", "skills", "dice_mechanics", "creationBudget",
        "attributeCosts", "packages", "traits", "advancement", "conditions");

    private final RuleSchemaValidator validator = new RuleSchemaValidator(new ObjectMapper());

    @Test
    void exampleSystemsValidateAgainstDefaultSchema() throws Exception {
        for (String file : List.of("dnd5e.json", "coc7e.json", "dsa5.json", "dsa5-playtest.json")) {
            var raw = Files.readString(Path.of("../docs/examples/" + file));
            var parsed = new ObjectMapper().readTree(raw);
            var filtered = new ObjectMapper().createObjectNode();
            RULES_KEYS.forEach(k -> {
                if (parsed.has(k)) filtered.set(k, parsed.get(k));
            });
            var errors = validator.validate(filtered.toString(), RuleSchemaValidator.DEFAULT_SCHEMA);
            assertThat(errors).as(file + " muss schema-konform sein: " + errors).isEmpty();
        }
    }
}
