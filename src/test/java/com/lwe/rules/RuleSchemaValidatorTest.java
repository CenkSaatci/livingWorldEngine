package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prüft die JSON-Schema-Validierung gegen die Beispiel-Regelwerke.
 */
class RuleSchemaValidatorTest {

    private RuleSchemaValidator validator;
    private String schemaJson;

    @BeforeEach
    void setUp() throws IOException {
        validator = new RuleSchemaValidator(new ObjectMapper());

        // Lade das Referenz-Schema aus den Test-Ressourcen (wird in P1-T05 erstellt)
        // Für den Unit-Test nutzen wir ein eingebettetes Minimal-Schema
        schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["version", "attributes", "dice_mechanics"],
              "additionalProperties": false,
              "properties": {
                "version": { "type": "integer", "minimum": 1 },
                "attributes": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "required": ["name", "type", "default"],
                    "properties": {
                      "name": { "type": "string" },
                      "type": { "enum": ["INT", "FLOAT", "STRING", "BOOL"] },
                      "min": { "type": "number" },
                      "max": { "type": "number" },
                      "default": { }
                    }
                  }
                },
                "skills": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "required": ["name", "attribute"],
                    "properties": {
                      "name": { "type": "string" },
                      "attribute": { "type": "string" },
                      "bonus": { "type": "integer", "default": 0 }
                    }
                  }
                },
                "dice_mechanics": {
                  "type": "object",
                  "required": ["probe"],
                  "properties": {
                    "probe": { "type": "string" },
                    "combat": {
                      "type": "object",
                      "required": ["initiative", "damage"],
                      "properties": {
                        "initiative": { "type": "string" },
                        "damage": { "type": "string" },
                        "action_points": {
                          "type": "object",
                          "properties": {
                            "standard": { "type": "integer", "default": 2 },
                            "max": { "type": "integer", "default": 4 }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;
    }

    @Test
    void shouldValidateD20Lite() throws IOException {
        var json = loadFixture("d20lite.json");
        var errors = validator.validate(json, schemaJson);
        assertThat(errors).as("D20Lite should be schema-valid").isEmpty();
    }

    @Test
    void shouldValidateTwoDicePool() throws IOException {
        var json = loadFixture("twodicepool.json");
        var errors = validator.validate(json, schemaJson);
        assertThat(errors).as("TwoDicePool should be schema-valid").isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredField() {
        var invalid = """
            {"version": 1, "attributes": []}
            """;
        var errors = validator.validate(invalid, schemaJson);
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidAttributeType() {
        var invalid = """
            {"version":1,"attributes":[{"name":"x","type":"BOOLEAN","default":1}],"dice_mechanics":{"probe":"1d20"}}
            """;
        var errors = validator.validate(invalid, schemaJson);
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldThrowOnInvalidInput() {
        assertThatThrownBy(() -> validator.validateOrThrow("not json", schemaJson))
            .isInstanceOf(RuleSchemaValidator.SchemaValidationException.class);
    }

    private String loadFixture(String name) throws IOException {
        return Files.readString(Path.of("src/test/resources/rules/" + name));
    }
}