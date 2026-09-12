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
            "version":          { "type": "integer", "minimum": 1 },
            "probeType":        { "type": "string", "enum": ["d20_target", "d100_threshold", "d20_3attr"] },
            "progressionType":  { "type": "string" },
            "features":         { "type": "object" },
            "modifierFormula":  { "type": "string" },
            "creationBudget":   { "type": "object", "properties": {
              "ap":               { "type": "integer", "minimum": 0 },
              "apCarryoverMax":   { "type": "integer", "minimum": 0 },
              "fatePoints":       { "type": "integer", "minimum": 0 },
              "attrBase":         { "type": "integer" },
              "maxAttrTotal":     { "type": "integer", "minimum": 0 },
              "maxAttrValue":     { "type": "integer", "minimum": 0 },
              "maxSkillValue":    { "type": "integer", "minimum": 0 },
              "maxCombatValue":   { "type": "integer", "minimum": 0 },
              "maxSpells":        { "type": "integer", "minimum": 0 },
              "maxAdvantageAp":   { "type": "integer", "minimum": 0 }
            } },
            "attributeCosts":   { "type": "object", "properties": {
              "default": { "type": "array", "items": { "$ref": "#/$defs/costTier" } }
            } },
            "packages":         { "type": "array", "items": { "$ref": "#/$defs/package" } },
            "traits":           { "type": "array", "items": { "$ref": "#/$defs/trait" } },
            "advancement":      { "type": "object", "properties": {
              "columns":   { "type": "array", "items": { "type": "string" } },
              "table":     { "type": "array", "items": { "$ref": "#/$defs/advancementRow" } },
              "maxRule":   { "type": "string" }
            } },
            "derived_values":   { "type": "array", "items": { "type": "object" } },
            "abilities":        { "type": "array", "items": {
                "type": "object",
                "properties": {
                    "name":       { "type": "string" },
                    "damageType": { "type": "string" }
                }
            } },
            "progression":      { "type": "object" },
            "magic":            { "type": "object" },
            "psionics":         { "type": "object" },
            "conditionals":     { "type": "array", "items": { "type": "object" } },
            "conditions":       { "type": "array", "items": { "$ref": "#/$defs/condition" } },
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
                "probe":        { "type": "string" },
                "difficulties": { "type": "array", "items": {
                  "type": "object", "required": ["name"], "properties": {
                    "name":       { "type": "string", "minLength": 1 },
                    "multiplier": { "type": "number", "exclusiveMinimum": 0 },
                    "delta":      { "type": "integer" }
                  }
                } },
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
                "default": { },
                "costs":   { "type": "array", "items": { "$ref": "#/$defs/costTier" } }
              }
            },
            "skill": {
              "type": "object",
              "required": ["name"],
              "anyOf": [
                { "required": ["attribute"] },
                { "required": ["attributes"] }
              ],
              "properties": {
                "name":       { "type": "string" },
                "attribute":  { "type": "string", "description": "Legacy single attribute reference" },
                "attributes": { "type": "array", "items": { "type": "string" }, "description": "Multi-attribute reference" },
                "bonus":      { "type": "integer", "default": 0 },
                "costColumn": { "type": "string" },
                "activationCost": { "type": "integer", "minimum": 0 },
                "casting": { "type": "object", "properties": {
                  "resource": { "type": "string", "pattern": "^[a-z][a-z0-9_]{0,30}$" },
                  "cost":     { "type": "integer", "minimum": 1 },
                  "requiresTrait": { "type": "string" },
                  "restore":  { "type": "string", "enum": ["short", "long"] }
                } }
              }
            },
            "combat": {
              "type": "object",
              "required": ["initiative", "damage"],
              "properties": {
                "initiative":        { "type": "string" },
                "damage":            { "type": "string" },
                "action_points":     { "$ref": "#/$defs/actionPoints" },
                "action_types":      { "type": "array", "items": { "type": "string" } },
                "actions_per_turn":  { "type": "object" },
                "maneuvers":         { "type": "array", "items": { "$ref": "#/$defs/maneuver" } },
                "critical_hit":      { "type": "object", "properties": {
                  "threshold": { "type": "integer", "minimum": 1, "maximum": 20 },
                  "multiplier": { "type": "integer", "minimum": 1 }
                } },
                "saving_throws":     { "type": "object", "properties": {
                  "base_dc": { "type": "integer" },
                  "proficiency_bonus": { "type": "string" }
                } },
                "attack":            { "type": "object", "required": ["attribute", "target"], "properties": {
                  "attribute":  { "type": "string", "minLength": 1 },
                  "target":     { "type": "string", "minLength": 1 },
                  "dice":       { "type": "string" },
                  "comparison": { "type": "string", "enum": ["gte", "lte"] }
                } },
                "resting":           { "type": "object", "properties": {
                  "short_rest": { "type": "object", "properties": {
                    "heal_percent": { "type": "number", "minimum": 0, "maximum": 1 },
                    "recover_resources": { "type": "boolean" }
                  } },
                  "long_rest": { "type": "object", "properties": {
                    "full_heal": { "type": "boolean" },
                    "recover_all": { "type": "boolean" }
                  } }
                } }
              }
            },
            "actionPoints": {
              "type": "object",
              "properties": {
                "standard": { "type": "integer", "default": 2 },
                "max":      { "type": "integer", "default": 4 }
              }
            },
            "costTier": {
              "type": "object",
              "required": ["upTo", "cost"],
              "properties": {
                "upTo": { "type": "integer" },
                "cost": { "type": "integer", "minimum": 0 }
              }
            },
            "trait": {
              "type": "object",
              "required": ["name", "kind"],
              "properties": {
                "name":     { "type": "string", "minLength": 1 },
                "kind":     { "enum": ["advantage", "disadvantage"] },
                "costs":    { "type": "array", "items": { "$ref": "#/$defs/traitCost" } },
                "requires": { "type": "array", "items": { "type": "string" } },
                "excludes": { "type": "array", "items": { "type": "string" } },
                "effects":  { "type": "array", "items": { "$ref": "#/$defs/traitEffect" } }
              }
            },
            "traitCost": {
              "type": "object",
              "required": ["tier", "cost"],
              "properties": {
                "tier": { "type": "string" },
                "cost": { "type": "integer" }
              }
            },
            "traitEffect": {
              "type": "object",
              "required": ["target", "op", "value"],
              "properties": {
                "target": { "type": "string" },
                "op":     { "enum": ["add"] },
                "value":  { "type": "number" }
              }
            },
            "package": {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name":          { "type": "string", "minLength": 1 },
                "kind":          { "type": "string" },
                "cost":          { "type": "integer" },
                "attributeMods": { "type": "array", "items": {
                  "type": "object",
                  "required": ["value"],
                  "properties": {
                    "attr":   { "type": "string" },
                    "choice": { "oneOf": [
                      { "type": "string" },
                      { "type": "array", "items": { "type": "string" } }
                    ] },
                    "value":  { "type": "integer" }
                  }
                } },
                "autoTraits":    { "type": "array", "items": { "type": "string" } },
                "baseValues":    { "type": "array", "items": {
                  "type": "object",
                  "required": ["name", "value"],
                  "properties": {
                    "name":  { "type": "string" },
                    "value": { "type": "number" }
                  }
                } },
                "recommended":   { "type": "array", "items": { "type": "string" } },
                "restricted":    { "type": "array", "items": { "type": "string" } }
              }
            },
            "maneuver": {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name":        { "type": "string", "minLength": 1 },
                "attackMalus": { "type": "integer" },
                "damageType":  { "type": "string" },
                "apCost":      { "type": "integer", "minimum": 1 },
                "effects":     { "type": "array", "items": { "$ref": "#/$defs/traitEffect" } }
              }
            },
            "condition": {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name":    { "type": "string", "minLength": 1 },
                "rounds":  { "type": "integer", "minimum": 1 },
                "effects": { "type": "array", "items": { "$ref": "#/$defs/traitEffect" } }
              }
            },
            "advancementRow": {
              "type": "object",
              "required": ["from", "to"],
              "properties": {
                "from":  { "type": "integer" },
                "to":    { "type": "integer" },
                "costs": { "type": "object", "additionalProperties": { "type": "integer", "minimum": 0 } }
              }
            },
            "diceExpression": {
              "type": "string",
              "pattern": "^[0-9]+d[0-9]+([+-][a-z_0-9]+)?$",
              "description": "Ausdr\u00fccke wie '1d20+mod', '2d6+intelligenz', '1d8+st\u00e4rke', '1d20'"
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