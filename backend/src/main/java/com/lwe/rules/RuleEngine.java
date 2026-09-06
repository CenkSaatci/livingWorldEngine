package com.lwe.rules;

/**
 * Zentrales Interface der Rule-Engine. Implementierungen lesen ihre Konfiguration aus
 * einem {@code GameSystem.rulesJson}.
 *
 * <p>Jedes Dice-System (D20, Pool, …) hat eine eigene Implementierung.
 *
 * @see <a href="../../../docs/RULES-SCHEMA.md">docs/RULES-SCHEMA.md</a>
 */
public interface RuleEngine {

    DiceExpressionParser.DiceSystem getDiceSystem();

    /**
     * Führt eine Probe (Skill-Check) auf Basis der übergebenen Parameter aus.
     *
     * @param request Probe-Parameter (Skill, Attributswerte, Modifier)
     * @return Ergebnis (Würfelaugen, Total, Erfolg)
     */
    ProbeResult executeProbe(ProbeRequest request);

    /**
     * Berechnet den Wert eines Attributs (z. B. {@code floor((staerke - 10) / 2)} für D20-Modifier).
     */
    int calculateModifier(int attributeValue);

    record ProbeRequest(String skillId, int attributeValue, int modifier, int target, String diceExpression) {
        public ProbeRequest(String skillId, int attributeValue, int modifier, int target) {
            this(skillId, attributeValue, modifier, target, "1d20");
        }
    }
    /**
     * Ergebnis einer Probe.
     *
     * @param successTier 0 = Fehlschlag, 1 = bedingter Erfolg/Komplikation, 2 = Erfolg, 3 = großer Erfolg
     */
    record ProbeResult(String expression, int[] dice, int total, int target,
                       boolean success, int successTier) {

        public ProbeResult(String expression, int[] dice, int total, int target, boolean success) {
            this(expression, dice, total, target, success, success ? 2 : 0);
        }
    }
}