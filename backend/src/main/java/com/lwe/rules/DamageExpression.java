package com.lwe.rules;

import java.util.regex.Pattern;

/**
 * Zerlegt Schadensausdrücke wie {@code "1d6"}, {@code "1d6+2"}, {@code "1d8+staerke"},
 * {@code "1d8+staerke+2"} oder {@code "1d6-staerke"} in Würfel, Flat und Attribut-Term.
 * Leerzeichen sind erlaubt; mehrere Flat-Terme werden summiert; maximal ein Attribut-Term.
 * Zahlen außerhalb des int-Bereichs und nicht trennbare Identifier machen den Ausdruck
 * unparsbar (null). Groß-/Kleinschreibung des Würfelteils ist tolerant ("1D6").
 * Gemeinsame Quelle für CombatService und NPC-IntentExecutor (ADR-014).
 */
public final class DamageExpression {

    /** {@code attrBonusSign} ist +1 oder -1 (Ausdrücke wie "1d6-staerke"). */
    public record Parts(String dice, String attr, int flat, int attrBonusSign) {
        public Parts(String dice, String attr, int flat) {
            this(dice, attr, flat, 1);
        }
    }

    private static final Pattern EXPR =
        Pattern.compile("^(\\d+d\\d+)(.*)$", Pattern.CASE_INSENSITIVE);
    /** Optionaler Whitespace um Vorzeichen und Term; erkennt verschmolzene Identifier nicht. */
    private static final Pattern TERM = Pattern.compile(
        "\\s*([+-])\\s*(\\d+|[A-Za-z_äöüÄÖÜß][\\wäöüÄÖÜß]*)\\s*");

    private DamageExpression() {}

    /** @return null bei unparsbarem Ausdruck (Aufrufer entscheidet: Default vs. Fehler). */
    public static Parts parse(String expr) {
        if (expr == null || expr.isBlank()) return null;
        var m = EXPR.matcher(expr.strip());
        if (!m.matches()) return null;
        var dice = m.group(1);
        var rest = m.group(2);
        if (rest == null || rest.isBlank()) return new Parts(dice, null, 0, 1);

        String attr = null;
        int attrBonusSign = 1;
        long flat = 0;
        int pos = 0;
        var term = TERM.matcher(rest);
        while (term.find()) {
            if (term.start() != pos) return null; // Lücke / ungültiges Zeichen
            pos = term.end();
            int sign = "-".equals(term.group(1)) ? -1 : 1;
            var token = term.group(2);
            if (token.chars().allMatch(Character::isDigit)) {
                long value;
                try {
                    value = sign * Long.parseLong(token);
                } catch (NumberFormatException e) {
                    return null;
                }
                flat += value;
                if (flat > Integer.MAX_VALUE || flat < Integer.MIN_VALUE) return null;
            } else {
                if (attr != null) return null; // nur ein Attribut-Term
                attr = token;
                attrBonusSign = sign;
            }
        }
        if (pos != rest.length()) return null;
        return new Parts(dice, attr, (int) flat, attrBonusSign);
    }
}
