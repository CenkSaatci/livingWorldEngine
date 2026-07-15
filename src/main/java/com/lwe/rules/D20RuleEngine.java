package com.lwe.rules;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Component
public class D20RuleEngine implements RuleEngine {

    @Override
    public DiceExpressionParser.DiceSystem getDiceSystem() { return DiceExpressionParser.DiceSystem.D20; }

    private static final Pattern DICE_PATTERN = Pattern.compile("(\\d+)d(\\d+)");

    @Override
    public int calculateModifier(int attributeValue) {
        return Math.floorDiv(attributeValue - 10, 2);
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = calculateModifier(request.attributeValue()) + request.modifier();
        int sides = 20, count = 1;
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
        return new ProbeResult(expr, rolls, total, request.target(), total >= request.target());
    }
}