package com.lwe.rules;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

/**
 * D20-basierte Rule-Engine (angelehnt an D&D 5e/3.x).
 *
 * <p>Würfelt 1W20, addiert den Attribut-Modifikator {@code floor((attr - 10) / 2)} und
 * optionalen Bonus. Vergleich mit {@code target} → {@code total >= target} ist Erfolg.
 */
@Component
public class D20RuleEngine implements RuleEngine {

    @Override
    public int calculateModifier(int attributeValue) {
        return Math.floorDiv(attributeValue - 10, 2);
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = calculateModifier(request.attributeValue()) + request.modifier();
        var roll = ThreadLocalRandom.current().nextInt(1, 21);
        var total = roll + mod;
        var expr = "1d20" + (mod >= 0 ? "+" : "") + mod;
        return new ProbeResult(expr, new int[]{roll}, total, request.target(), total >= request.target());
    }
}