package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validiert ein {@code rules_json} (Game System) gegen ein JSON Schema aus
 * {@code schema_json}. Nutzt die Networknt-JSON-Schema-Validator-Bibliothek.
 *
 * @see <a href="../../../docs/RULES-SCHEMA.md">docs/RULES-SCHEMA.md</a>
 */
@Component
public class RuleSchemaValidator {

    public static final String DEFAULT_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "GameSystem",
          "type": "object",
          "required": ["version", "attributes", "dice_mechanics"],
          "additionalProperties": false,
          "properties": {
            "version": { "type": "integer", "minimum": 1 },
            "attributes": {
              "type": "array",
              "minItems": 1,
              "items": { "$ref": "#/$defs/attribute" }
            },
            "skills": {
              "type": "array",
              "items": { "$ref": "#/$defs/skill" }
            },
            "dice_mechanics": {
              "type": "object",
              "required": ["probe"],
              "properties": {
                "probe":        { "type": "string", "$ref": "#/$defs/diceExpression" },
                "combat":       { "$ref": "#/$defs/combat" }
              }
            }
          },
          "$defs": {
            "attribute": {
              "type": "object",
              "required": ["name", "type", "default"],
              "properties": {
                "name":    { "type": "string", "minLength": 1, "maxLength": 50 },
                "type":    { "enum": ["INT", "FLOAT", "STRING", "BOOL"] },
                "min":     { "type": "number" },
                "max":     { "type": "number" },
                "default": { }
              }
            },
            "skill": {
              "type": "object",
              "required": ["name", "attribute"],
              "properties": {
                "name":      { "type": "string" },
                "attribute": { "type": "string", "description": "Referenz auf Attribut #/properties/attributes/items/properties/name" },
                "bonus":     { "type": "integer", "default": 0 }
              }
            },
            "combat": {
              "type": "object",
              "required": ["initiative", "damage"],
              "properties": {
                "initiative":     { "$ref": "#/$defs/diceExpression" },
                "damage":         { "$ref": "#/$defs/diceExpression" },
                "action_points":  { "$ref": "#/$defs/actionPoints" }
              }
            },
            "actionPoints": {
              "type": "object",
              "properties": {
                "standard": { "type": "integer", "default": 2 },
                "max":      { "type": "integer", "default": 4 }
              }
            },
            "diceExpression": {
              "type": "string",
              "pattern": "^[0-9]+d[0-9]+([+-][a-z_0-9]+)([+-][0-9]+)?$",
              "description": "Ausdr\u00fccke wie '1d20+mod', '2d6+intelligenz', '1d8+st\u00e4rke'"
            }
          }
        }
        """;

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory factory;

    public RuleSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Validiert {@code rulesJson} gegen {@code schemaJson}.
     *
     * @return Liste von Validierungsfehlern (leer = gültig)
     */
    public List<ValidationError> validate(String rulesJson, String schemaJson) {
        try {
            var schemaNode = objectMapper.readTree(schemaJson);
            var schema = factory.getSchema(schemaNode);

            var rulesNode = objectMapper.readTree(rulesJson);
            var errors = schema.validate(rulesNode);

            return errors.stream()
                .map(e -> new ValidationError(e.getInstanceLocation().toString(), e.getMessage()))
                .toList();
        } catch (Exception e) {
            return List.of(new ValidationError("(root)", "Parsing error: " + e.getMessage()));
        }
    }

    /**
     * Validiert und wirft bei Fehlern eine Exception.
     */
    public void validateOrThrow(String rulesJson, String schemaJson) {
        var errors = validate(rulesJson, schemaJson);
        if (!errors.isEmpty()) {
            throw new SchemaValidationException(errors);
        }
    }

    public record ValidationError(String path, String message) {}

    public static class SchemaValidationException extends RuntimeException {
        private final List<ValidationError> errors;
        public SchemaValidationException(List<ValidationError> errors) {
            super("Schema validation failed: " + errors.size() + " error(s)");
            this.errors = errors;
        }
        public List<ValidationError> getErrors() { return errors; }
    }
}