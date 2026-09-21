package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-015: Schema/Semantik für {@code currency} und {@code poi_actions}.
 */
class PoiActionsSchemaTest {

    private final RuleSchemaValidator validator = new RuleSchemaValidator(new ObjectMapper());

    private static String rules(String extra) {
        return """
            {
              "version": 1,
              "attributes": [ { "name": "mut", "type": "INT", "default": 10 } ],
              "dice_mechanics": { "probe": "1d20" }
              %s
            }
            """.formatted(extra.isEmpty() ? "" : "," + extra);
    }

    private java.util.List<RuleSchemaValidator.ValidationError> validate(String json) {
        return validator.validate(json, RuleSchemaValidator.DEFAULT_SCHEMA);
    }

    @Test
    void acceptsCurrencyAndActions() {
        var errors = validate(rules("""
            "currency": { "name": "Dukaten", "denominations": [
              { "name": "Kupfer", "abbr": "K", "factor": 1 },
              { "name": "Gold", "abbr": "G", "factor": 100 } ] },
            "poi_actions": [
              { "name": "Medicus", "chat": "actor", "effects": [
                { "type": "money", "amount": -15 },
                { "type": "heal", "amount": "2d6" },
                { "type": "condition", "name": "Wunde", "remove": true } ] },
              { "name": "Wirtshaus", "chat": "public", "effects": [
                { "type": "money", "amount": -5 }, { "type": "rest", "mode": "long" } ] },
              { "name": "Handeln", "trade": { "buy": true, "sell": true, "sellRate": 0.5 } },
              { "name": "Aussichtspunkt", "description": "Weitblick", "effects": [] }
            ]
            """));
        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsDuplicateDenomination() {
        var errors = validate(rules("""
            "currency": { "denominations": [
              { "name": "Gold", "factor": 100 },
              { "name": "gold", "factor": 10 } ] }
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("Doppelte Sorte"));
    }

    @Test
    void rejectsInvalidFactor() {
        var errors = validate(rules("""
            "currency": { "denominations": [ { "name": "Gold", "factor": 0 } ] }
            """));
        assertThat(errors).isNotEmpty();
    }

    @Test
    void rejectsDuplicateActionName() {
        var errors = validate(rules("""
            "poi_actions": [
              { "name": "Medicus" },
              { "name": "medicus" } ]
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("Doppelte Aktion"));
    }

    @Test
    void rejectsMoneyEffectWithoutAmount() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "Spende", "effects": [ { "type": "money" } ] } ]
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("money-Effekt"));
    }

    @Test
    void rejectsBrokenHealExpression() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "Medicus", "effects": [ { "type": "heal", "amount": "viel" } ] } ]
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("heal-Effekt"));
    }

    @Test
    void rejectsItemEffectWithoutNameOrQuantity() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "Fund", "effects": [ { "type": "item", "qty": 1 } ] } ]
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("item-Effekt"));
    }

    @Test
    void rejectsRestEffectWithoutMode() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "Rast", "effects": [ { "type": "rest" } ] } ]
            """));
        assertThat(errors).anyMatch(e -> e.message().contains("rest-Effekt"));
    }

    @Test
    void rejectsUnknownEffectType() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "Zauber", "effects": [ { "type": "teleport" } ] } ]
            """));
        assertThat(errors).isNotEmpty();
    }

    @Test
    void rejectsUnknownActionField() {
        var errors = validate(rules("""
            "poi_actions": [ { "name": "X", "kind": "probe" } ]
            """));
        // additionalProperties ist in $defs nicht gesetzt → unbekannte Felder sind erlaubt;
        // dieser Test dokumentiert, dass wir (noch) nicht strikt sind.
        assertThat(errors).isEmpty();
    }
}
