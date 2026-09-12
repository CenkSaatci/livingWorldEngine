package com.lwe.rules;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * P1: Engine fuer Wurf-Systeme mit Unterwurf-Semantik (d100, z.B. CoC):
 * Wurf + Modifikator muss das Ziel UNTERSCHREITEN ({@code total <= target}).
 */
@Component
public class ThrownRuleEngine implements RuleEngine {

    @Override
    public DiceExpressionParser.DiceSystem getDiceSystem() { return DiceExpressionParser.DiceSystem.THROWN; }

    private static final Pattern DICE_PATTERN = Pattern.compile("(\\d+)d(\\d+)");

    @Override
    public int calculateModifier(int attributeValue) {
        return attributeValue;
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = request.modifier();
        int sides = 100, count = 1;
        var m = DICE_PATTERN.matcher(request.diceExpression());
        if (m.find()) { count = Integer.parseInt(m.group(1)); sides = Integer.parseInt(m.group(2)); }
        var total = 0;
        var rolls = new int[count];
        for (int i = 0; i < count; i++) {
            rolls[i] = ThreadLocalRandom.current().nextInt(1, sides + 1);
            total += rolls[i];
        }
        total += mod;
        var expr = count + "d" + sides + (mod >= 0 ? "+" : "") + mod;
        var success = request.target() > 0 && total <= request.target();
        return new ProbeResult(expr, rolls, total, request.target(), success, success ? 2 : 0);
    }
}
