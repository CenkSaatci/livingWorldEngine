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
                        },
                        "critical_hit": { "type": "object" },
                        "saving_throws": { "type": "object" },
                        "resting": { "type": "object" }
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
    void shouldAcceptNewCombatFields() {
        var json = """
            {"version":1,"attributes":[{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],"dice_mechanics":{
              "probe":"1d20+mod",
              "combat":{
                "initiative":"1d20+geschick",
                "damage":"1d8+staerke",
                "critical_hit":{"threshold":20,"multiplier":2},
                "saving_throws":{"base_dc":8,"proficiency_bonus":"floor((attr-10)/2)"},
                "resting":{"short_rest":{"heal_percent":0.5,"recover_resources":true},"long_rest":{"full_heal":true,"recover_all":true}}
              }
            }}
            """;
        var errors = validator.validate(json, schemaJson);
        assertThat(errors).as("Neue Combat-Felder sollten akzeptiert werden").isEmpty();
    }


    @Test
    void shouldAcceptDamageTypesOnAbilitiesAndManeuvers() throws IOException {
        var ok = """
            {
              "version": 1,
              "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
              "dice_mechanics": {
                "probe": "1d20+mod",
                "combat": {
                  "initiative": "1d20",
                  "damage": "1d8",
                  "maneuvers": [{"name":"Wuchtschlag","apCost":2,"damageType":"bludgeoning"}]
                }
              },
              "abilities": [
                {"name":"Angriff (Fernkampf)","type":"active","costType":"AP","cost":1,
                 "damageType":"piercing"}
              ]
            }
            """;
        assertThat(validator.validate(ok, RuleSchemaValidator.DEFAULT_SCHEMA)).isEmpty();

        var bad = ok.replace("\"piercing\"", "5");
        assertThat(validator.validate(bad, RuleSchemaValidator.DEFAULT_SCHEMA)).isNotEmpty();
    }


    @Test
    void shouldValidatePackageShapes() throws IOException {
        var ok = """
            {
              "version": 1,
              "attributes": [{"name":"MU","type":"INT","min":3,"max":20,"default":10}],
              "dice_mechanics": {"probe": "1d20+mod"},
              "packages": [
                {
                  "name": "Elf", "kind": "species", "cost": 18,
                  "attributeMods": [
                    {"attr": "MU", "value": 1},
                    {"choice": ["KK", "KO"], "value": -1},
                    {"choice": "*", "value": 1}
                  ],
                  "autoTraits": ["Nachtsicht"],
                  "baseValues": [{"name": "LE", "value": 8}],
                  "recommended": ["Waldelf"],
                  "restricted": ["Zwerg"]
                }
              ]
            }
            """;
        assertThat(validator.validate(ok, RuleSchemaValidator.DEFAULT_SCHEMA)).isEmpty();

        var badChoice = ok.replace("\"choice\": [\"KK\", \"KO\"]", "\"choice\": 5");
        assertThat(validator.validate(badChoice, RuleSchemaValidator.DEFAULT_SCHEMA)).isNotEmpty();
        var missingValue = ok.replace("{\"attr\": \"MU\", \"value\": 1}", "{\"attr\": \"MU\"}");
        assertThat(validator.validate(missingValue, RuleSchemaValidator.DEFAULT_SCHEMA)).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidTraitShapes() {
        // P28-T03: Traits sind jetzt streng typisiert.
        var badKind = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "traits":[{"name":"Glück","kind":"banana"}]}
            """;
        assertThat(validator.validate(badKind, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("unknown kind should fail").isNotEmpty();

        var badOp = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "traits":[{"name":"Glück","kind":"advantage","effects":[{"target":"derived:hp","op":"multiply","value":2}]}]}
            """;
        assertThat(validator.validate(badOp, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("unknown effect op should fail").isNotEmpty();

        var ok = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "traits":[{"name":"Glück","kind":"advantage",
               "costs":[{"tier":"I","cost":30}],
               "requires":["Zauberer"],"excludes":["Pech"],
               "effects":[{"target":"derived:hp","op":"add","value":3}]}]}
            """;
        assertThat(validator.validate(ok, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("valid trait should pass").isEmpty();
    }

    @Test
    void shouldRejectInvalidAdvancementShapes() {
        // P28-T04: advancement + skill.costColumn/activationCost streng typisiert.
        var badRow = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "advancement":{"columns":["A"],"table":[{"from":1,"to":12,"costs":{"A":-3}}]}}
            """;
        assertThat(validator.validate(badRow, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("negative advancement cost should fail").isNotEmpty();

        var badActivation = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "skills":[{"name":"Zauber","attributes":["x"],"activationCost":"viel"}]}
            """;
        assertThat(validator.validate(badActivation, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("string activationCost should fail").isNotEmpty();

        var ok = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "skills":[{"name":"Zauber","attributes":["x"],"costColumn":"B","activationCost":3}],
             "advancement":{"columns":["A","B"],"maxRule":"highestAttributePlus2",
               "table":[{"from":1,"to":12,"costs":{"A":1,"B":2}},{"from":13,"to":13,"costs":{"A":2,"B":4}}]}}
            """;
        assertThat(validator.validate(ok, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("valid advancement should pass").isEmpty();
    }

    @Test
    void shouldValidateDsa5ExampleAgainstDefaultSchema() throws IOException {
        // P29-T06: Referenz-Content (P23/P28/P29) muss schema-valide sein.
        var json = java.nio.file.Files.readString(java.nio.file.Path.of("../docs/examples/dsa5.json"));
        assertThat(validator.validate(json, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("DSA5-Referenz muss gegen DEFAULT_SCHEMA validieren").isEmpty();
    }

    @Test
    void shouldValidateP28ReferenceAgainstDefaultSchema() throws IOException {
        // P28-T06: Referenz-System nutzt alle P28-Bloecke.
        var json = loadFixture("p28-reference.json");
        var errors = validator.validate(json, RuleSchemaValidator.DEFAULT_SCHEMA);
        assertThat(errors).as("P28-Referenz muss gegen DEFAULT_SCHEMA validieren").isEmpty();
    }


    @Test
    void shouldRejectInvalidConditionRounds() {
        var bad = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "conditions":[{"name":"Wunde","rounds":0}]}
            """;
        assertThat(validator.validate(bad, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("rounds must be >= 1").isNotEmpty();

        var ok = """
            {"version":1,"attributes":[{"name":"x","type":"INT","default":1}],
             "dice_mechanics":{"probe":"1d20"},
             "conditions":[{"name":"Wunde","effects":[{"target":"probe","op":"add","value":-4}]}]}
            """;
        assertThat(validator.validate(ok, RuleSchemaValidator.DEFAULT_SCHEMA)).isEmpty();
    }
    @Test
    void shouldThrowOnInvalidInput() {
        assertThatThrownBy(() -> validator.validateOrThrow("not json", schemaJson))
            .isInstanceOf(RuleSchemaValidator.SchemaValidationException.class);
    }

    @Test
    void shouldValidateD20LiteAgainstDefaultSchema() throws IOException {
        // Regression (P28-T01): alte Systeme müssen gegen das echte
        // Backend-Schema validieren, nicht nur gegen das Test-Minimalschema.
        var errors = validator.validate(loadFixture("d20lite.json"),
            RuleSchemaValidator.DEFAULT_SCHEMA);
        assertThat(errors).as("D20Lite should validate against DEFAULT_SCHEMA").isEmpty();
    }

    @Test
    void shouldAcceptP28BlocksAgainstDefaultSchema() {        // P28-T01: neue Top-Level-Blöcke als permissive Container.
        var json = """
            {"version":1,
             "attributes":[{"name":"staerke","type":"INT","min":1,"max":20,"default":10}],
             "dice_mechanics":{"probe":"1d20+mod"},
             "creationBudget":{"ap":1100,"maxAttrTotal":100},
             "attributeCosts":{"default":[{"upTo":14,"cost":15}]},
             "packages":[{"name":"Elf","kind":"species","cost":18}],
             "traits":[{"name":"Glück","kind":"advantage","costs":[{"tier":"I","cost":30}]}],
             "advancement":{"columns":["A","B"],"table":[]},
             "derived_values":[{"name":"sk","input":"mut+klugheit+intuition",
               "table":[{"min":24,"max":26,"value":4}],"requiresTrait":"Zauberer"}]}
            """;
        var errors = validator.validate(json, RuleSchemaValidator.DEFAULT_SCHEMA);
        assertThat(errors).as("P28 blocks should validate against DEFAULT_SCHEMA").isEmpty();
    }

    @Test
    void shouldRejectInvalidBudgetShapes() {
        // P28-T02: Budgets/Kosten mit falschen Typen sind Formfehler, kein "passt schon".
        var badAp = """
            {"version":1,
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "dice_mechanics":{"probe":"1d20"},
             "creationBudget":{"ap":"viel"}}
            """;
        assertThat(validator.validate(badAp, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("ap as string should fail").isNotEmpty();

        var badCost = """
            {"version":1,
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "dice_mechanics":{"probe":"1d20"},
             "attributeCosts":{"default":[{"upTo":14,"cost":-5}]}}
            """;
        assertThat(validator.validate(badCost, RuleSchemaValidator.DEFAULT_SCHEMA))
            .as("negative cost should fail").isNotEmpty();
    }

    private String loadFixture(String name) throws IOException {
        return Files.readString(Path.of("src/test/resources/rules/" + name));
    }
}