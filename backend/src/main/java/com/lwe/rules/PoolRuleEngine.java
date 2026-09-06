package com.lwe.rules;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Component("poolRuleEngine")
public class PoolRuleEngine implements RuleEngine {

    @Override
    public DiceExpressionParser.DiceSystem getDiceSystem() { return DiceExpressionParser.DiceSystem.POOL; }

    private static final Pattern DICE_PATTERN = Pattern.compile("(\\d+)d(\\d+)");

    @Override
    public int calculateModifier(int attributeValue) {
        return attributeValue;
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = calculateModifier(request.attributeValue()) + request.modifier();
        int sides = 6, count = 2;
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

        return new ProbeResult(expr, rolls, total, request.target(), tier >= 1, tier);
    }
}