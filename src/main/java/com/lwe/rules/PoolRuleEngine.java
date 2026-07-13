package com.lwe.rules;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pool-basierte Rule-Engine (2W6 + Attribut vs. Erfolgsstufen).
 *
 * <p>Erfolgsstufen:
 * <ul>
 *   <li>{@code total &lt; 6} → Fehlschlag (Tier 0)</li>
 *   <li>{@code total &gt;= 6} → Komplikation (Tier 1)</li>
 *   <li>{@code total &gt;= 8} → Erfolg (Tier 2)</li>
 *   <li>{@code total &gt;= 11} → Großer Erfolg (Tier 3)</li>
 * </ul>
 *
 * Der Modifikator ist der direkte Attributswert ({@code floor((x-10)/2)} wird nicht angewendet).
 */
@Component("poolRuleEngine")
public class PoolRuleEngine implements RuleEngine {

    @Override
    public int calculateModifier(int attributeValue) {
        return attributeValue;
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = calculateModifier(request.attributeValue()) + request.modifier();
        var roll1 = ThreadLocalRandom.current().nextInt(1, 7);
        var roll2 = ThreadLocalRandom.current().nextInt(1, 7);
        var total = roll1 + roll2 + mod;
        var expr = "2d6" + (mod >= 0 ? "+" : "") + mod;

        int tier;
        if (total < 6) {
            tier = 0;
        } else if (total < 8) {
            tier = 1;
        } else if (total < 11) {
            tier = 2;
        } else {
            tier = 3;
        }

        return new ProbeResult(expr, new int[]{roll1, roll2}, total, request.target(),
            tier >= 1, tier);
    }
}