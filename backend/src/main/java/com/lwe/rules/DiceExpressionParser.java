package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

public class DiceExpressionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EXPR_PATTERN = Pattern.compile("^(\\d+)d(\\d+)([+-]\\w+)?$");
    private static final Pattern FUDGE_PATTERN = Pattern.compile("^(\\d+)d[Ff]([+-]\\d+)?$");

    public enum DiceSystem { D20, POOL, THROWN, FUDGE }

    public static DiceSystem detect(String rulesJson) {
        try {
            var tree = MAPPER.readTree(rulesJson);
            var probe = tree.path("dice_mechanics").path("probe").asText("").strip();
            
            // Fudge-Erkennung
            if (FUDGE_PATTERN.matcher(probe).matches()) return DiceSystem.FUDGE;

            var m = EXPR_PATTERN.matcher(probe);
            if (!m.matches()) throw new IllegalArgumentException("Invalid dice expression: " + probe);
            int count = Integer.parseInt(m.group(1));
            int sides = Integer.parseInt(m.group(2));
            
            if (sides == 100) return DiceSystem.THROWN;
            if (count == 1 && sides == 20) return DiceSystem.D20;
            if (count >= 1 && sides >= 2) return DiceSystem.POOL; // Jedes XdY mit Y>=2 ist ein Pool
            throw new IllegalArgumentException("Unknown dice system: " + count + "d" + sides);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new IllegalArgumentException("Failed to parse rules JSON", e);
        }
    }

    public record DiceProbe(int count, int sides) {}

    public static DiceProbe parseProbe(String expression) {
        var m = EXPR_PATTERN.matcher(expression.strip());
        if (!m.matches()) throw new IllegalArgumentException("Invalid dice expression: " + expression);
        return new DiceProbe(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
    }
}
