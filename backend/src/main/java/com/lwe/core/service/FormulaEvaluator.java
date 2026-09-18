package com.lwe.core.service;

import java.util.*;
import java.util.function.BinaryOperator;

/**
 * Sicheres Auswerten mathematischer Ausdrücke mit {@code @{name}}-Variablen.
 *
 * <p>Grammar:
 * <pre>
 * expr   → term (('+' | '-') term)*
 * term   → factor (('*' | '/') factor)*
 * factor → NUMBER | '@{' IDENT '}' | IDENT | '(' expr ')' | FUNCTION '(' expr ',' expr ')'
 * </pre>
 *
 * <p>Erlaubte Funktionen: {@code min(a,b)}, {@code max(a,b)}, {@code floor(a)}.
 * Unbekannte Variablen oder Division durch 0 werfen {@code EvaluationException}.
 */
public class FormulaEvaluator {

    public static class EvaluationException extends RuntimeException {
        public EvaluationException(String message) { super(message); }
    }

    /** Schutz gegen DoS über Regel-JSON (ADR-014): lange/tief verschachtelte Ausdrücke. */
    public static final int MAX_LENGTH = 1000;
    public static final int MAX_DEPTH = 64;

    private final String input;
    private int pos;
    private int depth;
    private final Set<String> allowedVariables;

    public FormulaEvaluator(String input, Set<String> allowedVariables) {
        this.input = input.strip();
        if (this.input.length() > MAX_LENGTH) {
            throw new EvaluationException("Ausdruck zu lang (max " + MAX_LENGTH + " Zeichen)");
        }
        this.pos = 0;
        // Vergleich case-insensitiv (ADR-014): alles auf Kleinbuchstaben normalisieren.
        var lower = new HashSet<String>();
        for (var v : allowedVariables) lower.add(v.toLowerCase(Locale.ROOT));
        this.allowedVariables = lower;
    }

    public double evaluate(Map<String, Integer> variables) {
        var lower = new HashMap<String, Integer>();
        variables.forEach((k, v) -> lower.putIfAbsent(k.toLowerCase(Locale.ROOT), v));
        var result = expr(lower);
        if (pos < input.length()) {
            throw new EvaluationException("Unerwartetes Zeichen '" + input.charAt(pos) + "' an Position " + pos);
        }
        return result;
    }

    private double expr(Map<String, Integer> variables) {
        var result = term(variables);
        while (pos < input.length()) {
            skipSpace();
            if (pos >= input.length()) break;
            var c = input.charAt(pos);
            if (c == '+') { pos++; result += term(variables); }
            else if (c == '-') { pos++; result -= term(variables); }
            else break;
        }
        return result;
    }

    private double term(Map<String, Integer> variables) {
        var result = factor(variables);
        while (pos < input.length()) {
            skipSpace();
            if (pos >= input.length()) break;
            var c = input.charAt(pos);
            if (c == '*') { pos++; result *= factor(variables); }
            else if (c == '/') {
                pos++;
                var divisor = factor(variables);
                if (divisor == 0) throw new EvaluationException("Division durch 0");
                result /= divisor;
            }
            else break;
        }
        return result;
    }

    private double factor(Map<String, Integer> variables) {
        skipSpace();

        if (pos >= input.length()) throw new EvaluationException("Ausdruck unvollständig");

        var c = input.charAt(pos);

        // Zahl
        if (c == '-' || Character.isDigit(c)) {
            return number();
        }

        // '(' expr ')'
        if (c == '(') {
            pos++;
            enterNested();
            try {
                var result = expr(variables);
                skipSpace();
                if (pos >= input.length() || input.charAt(pos) != ')')
                    throw new EvaluationException("Fehlende schließende Klammer");
                pos++;
                return result;
            } finally {
                depth--;
            }
        }

        // Funktion: min(, max(, floor(
        if (pos + 4 <= input.length()) {
            var sub = input.substring(pos);
            if (sub.startsWith("min(")) { pos += 4; return func2(variables, Math::min); }
            if (sub.startsWith("max(")) { pos += 4; return func2(variables, Math::max); }
            if (sub.startsWith("floor(")) { pos += 6; return func1(variables, Math::floor); }
        }

        // Variable: @{name} oder name
        if (c == '@') {
            return atVariable(variables);
        }
        if (Character.isLetter(c)) {
            return bareVariable(variables);
        }

        throw new EvaluationException("Unerwartetes Zeichen '" + c + "' an Position " + pos);
    }

    private double number() {
        var start = pos;
        if (pos < input.length() && input.charAt(pos) == '-') pos++;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }
        return Double.parseDouble(input.substring(start, pos));
    }

    private double atVariable(Map<String, Integer> variables) {
        if (pos >= input.length() || input.charAt(pos) != '@')
            throw new EvaluationException("Erwarte '@'");
        pos++;
        // @{name} oder @name
        if (pos < input.length() && input.charAt(pos) == '{') {
            pos++;
            var name = readIdentifier();
            if (pos >= input.length() || input.charAt(pos) != '}')
                throw new EvaluationException("Erwarte '}' nach Variablenname");
            pos++;
            return resolveVariable(name, variables);
        }
        var name = readIdentifier();
        return resolveVariable(name, variables);
    }

    private double bareVariable(Map<String, Integer> variables) {
        var name = readIdentifier();
        return resolveVariable(name, variables);
    }

    private String readIdentifier() {
        var start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
            pos++;
        if (pos == start) throw new EvaluationException("Erwarte Variablenname an Position " + pos);
        return input.substring(start, pos);
    }

    private double resolveVariable(String name, Map<String, Integer> variables) {
        var key = name.toLowerCase(Locale.ROOT);
        if (!allowedVariables.contains(key))
            throw new EvaluationException("Unbekannte Variable '" + name + "'. Erlaubt: " + allowedVariables);
        return variables.getOrDefault(key, 0);
    }

    private void enterNested() {
        if (++depth > MAX_DEPTH) {
            throw new EvaluationException("Ausdruck zu tief verschachtelt (max " + MAX_DEPTH + ")");
        }
    }

    private double func1(Map<String, Integer> variables, java.util.function.DoubleUnaryOperator op) {
        enterNested();
        try {
            var arg = expr(variables);
            skipSpace();
            if (pos >= input.length() || input.charAt(pos) != ')')
                throw new EvaluationException("Fehlende schließende Klammer bei Funktion");
            pos++;
            return op.applyAsDouble(arg);
        } finally {
            depth--;
        }
    }

    private double func2(Map<String, Integer> variables, BinaryOperator<Double> op) {
        enterNested();
        try {
            var a = expr(variables);
            skipSpace();
            if (pos >= input.length() || input.charAt(pos) != ',')
                throw new EvaluationException("Erwarte ',' zwischen Funktionsargumenten");
            pos++;
            var b = expr(variables);
            skipSpace();
            if (pos >= input.length() || input.charAt(pos) != ')')
                throw new EvaluationException("Fehlende schließende Klammer bei Funktion");
            pos++;
            return op.apply(a, b);
        } finally {
            depth--;
        }
    }

    private void skipSpace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    /** Bequemlichkeitsmethode */
    public static double eval(String expression, Map<String, Integer> variables, Set<String> allowed) {
        return new FormulaEvaluator(expression, allowed).evaluate(variables);
    }

    public static double eval(String expression, Map<String, Integer> variables) {
        return eval(expression, variables, variables.keySet());
    }
}
