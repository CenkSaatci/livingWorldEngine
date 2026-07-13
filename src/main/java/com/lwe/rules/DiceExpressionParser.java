package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parst das {@code dice_mechanics}-Objekt aus einem Game-System-Regelwerk und
 * bestimmt, welche Rule-Engine für das System verwendet werden soll.
 */
public class DiceExpressionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum DiceSystem { D20, POOL, UNKNOWN }

    /**
     * Ermittelt das Würfelsystem anhand des Regelfelds {@code dice_mechanics.probe}.
     *
     * @param rulesJson der Inhalt von {@code game_systems.rules_json}
     * @return erkanntes Würfelsystem
     */
    public static DiceSystem detect(String rulesJson) {
        try {
            var tree = MAPPER.readTree(rulesJson);
            var probe = tree.path("dice_mechanics").path("probe").asText("");
            if (probe.contains("1d20")) return DiceSystem.D20;
            if (probe.contains("2d6") || probe.contains("3d6")) return DiceSystem.POOL;
            return DiceSystem.UNKNOWN;
        } catch (Exception e) {
            return DiceSystem.UNKNOWN;
        }
    }
}