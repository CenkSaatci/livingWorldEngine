package com.lwe.rules;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Fudge/Fate Rule Engine.
 * Würfelt 4dF (vier Fudge-Würfel), jeder Würfel ergibt -1, 0 oder +1.
 * Ergebnis: Summe der vier Würfel (-4 bis +4) + Modifikator.
 *
 * <p>Erkannt wird die Expression {@code 4dF} (case-insensitive).
 */
@Component
public class FudgeRuleEngine implements RuleEngine {

    @Override
    public DiceExpressionParser.DiceSystem getDiceSystem() { return DiceExpressionParser.DiceSystem.FUDGE; }

    private static final Pattern FUDGE_PATTERN = Pattern.compile("^(\\d+)d[Ff]([+-]\\d+)?$");

    @Override
    public int calculateModifier(int attributeValue) {
        return attributeValue;
    }

    @Override
    public ProbeResult executeProbe(ProbeRequest request) {
        var mod = calculateModifier(request.attributeValue()) + request.modifier();
        var expr = request.diceExpression();

        int count = 4;
        var m = FUDGE_PATTERN.matcher(expr);
        if (m.find()) {
            count = Integer.parseInt(m.group(1));
        }

        var rolls = new int[count];
        int sum = 0;
        var rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            rolls[i] = rng.nextInt(-1, 2); // -1, 0, oder +1
            sum += rolls[i];
        }
        var total = sum + mod;

        var exprStr = count + "dF" + (mod >= 0 ? "+" : "") + mod;
        return new ProbeResult(exprStr, rolls, total, request.target(), total >= request.target());
    }
}
