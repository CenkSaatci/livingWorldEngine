package com.lwe.rules;

import java.util.regex.Pattern;

/**
 * Zerlegt Schadensausdrücke wie {@code "1d6"}, {@code "1d6+2"} oder
 * {@code "1d8+staerke"} in Würfel + Flat + Attributname.
 * Gemeinsame Quelle für CombatService und NPC-IntentExecutor (ADR-014).
 */
public final class DamageExpression {

    public record Parts(String dice, String attr, int flat) {}

    private static final Pattern EXPR = Pattern.compile("^(\\d+d\\d+)(.*)$");
    private static final Pattern FLAT = Pattern.compile("^[+-]?(\\d+)$");
    private static final Pattern ATTR = Pattern.compile("^[+-]?([A-Za-z_äöüÄÖÜß][\\wäöüÄÖÜß]*)$");

    private DamageExpression() {}

    /** @return null bei unparsbarem Ausdruck (Aufrufer entscheidet: Default vs. Fehler). */
    public static Parts parse(String expr) {
        if (expr == null || expr.isBlank()) return null;
        var m = EXPR.matcher(expr.strip());
        if (!m.matches()) return null;
        var dice = m.group(1);
        var rest = m.group(2).strip();
        if (rest.isEmpty()) return new Parts(dice, null, 0);
        var num = FLAT.matcher(rest);
        if (num.matches()) {
            int sign = rest.startsWith("-") ? -1 : 1;
            return new Parts(dice, null, sign * Integer.parseInt(num.group(1)));
        }
        var attr = ATTR.matcher(rest);
        if (attr.matches()) return new Parts(dice, attr.group(1), 0);
        return null;
    }
}
