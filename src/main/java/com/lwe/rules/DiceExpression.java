package com.lwe.rules;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Parst und würfelt Dice-Expressions wie "2d6+3", "1d20", "3d8-2".
 * Ergebnis enthält Einzelwürfe + Summe.
 */
public class DiceExpression {

    private static final Pattern EXPR_PATTERN = Pattern.compile(
        "^(\\d+)d(\\d+)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE
    );

    private final int count;
    private final int sides;
    private final int modifier;
    private final int[] rolls;
    private final int total;

    public DiceExpression(String expression) {
        var m = EXPR_PATTERN.matcher(expression.strip());
        if (!m.matches())
            throw new IllegalArgumentException("Invalid dice expression: " + expression);

        this.count = Integer.parseInt(m.group(1));
        this.sides = Integer.parseInt(m.group(2));
        this.modifier = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;

        this.rolls = new int[count];
        int sum = 0;
        var rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            rolls[i] = rng.nextInt(1, sides + 1);
            sum += rolls[i];
        }
        this.total = sum + modifier;
    }

    public int getCount() { return count; }
    public int getSides() { return sides; }
    public int getModifier() { return modifier; }
    public int[] getRolls() { return rolls; }
    public int getTotal() { return total; }
}
