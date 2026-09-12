package com.lwe.core.service;

import com.lwe.api.dto.SheetResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wertet {@code derived_values} aus {@code rulesJson} aus.
 *
 * <p>Drei Formen (P28-T05):
 * <ul>
 *   <li>{@code formula} — Ausdruck aus Attributen</li>
 *   <li>{@code input} + {@code table} — Ausdruck → Tabellen-Lookup (Lücken = Fehler mit Grund)</li>
 *   <li>{@code requiresTrait} — Eintrag existiert nur mit passendem Trait (sonst ausgelassen)</li>
 * </ul>
 */
@Service
public class DerivedValueService {

    public List<SheetResponse.DerivedValueInfo> evaluate(
            List<Map<String, Object>> derivedValues,
            Map<String, Integer> attributeValues) {
        return evaluate(derivedValues, attributeValues, List.of());
    }

    public List<SheetResponse.DerivedValueInfo> evaluate(
            List<Map<String, Object>> derivedValues,
            Map<String, Integer> attributeValues,
            List<String> selectedTraits) {

        if (derivedValues == null) return List.of();
        var allowed = attributeValues.keySet();

        return derivedValues.stream()
            .map(dv -> evaluateOne(dv, attributeValues, allowed, selectedTraits))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private SheetResponse.DerivedValueInfo evaluateOne(
            Map<String, Object> dv, Map<String, Integer> attributeValues,
            Set<String> allowed, List<String> selectedTraits) {

        var name = (String) dv.getOrDefault("name", "");

        var requiresTrait = (String) dv.get("requiresTrait");
        if (requiresTrait != null && !hasTrait(selectedTraits, requiresTrait)) {
            return null; // existiert für diesen Charakter nicht
        }

        try {
            if (dv.get("table") instanceof List<?> table) {
                var input = dv.get("input") instanceof String s ? s : null;
                if (input == null || input.isBlank()) {
                    return new SheetResponse.DerivedValueInfo(name, 0, "Missing input expression");
                }
                var value = FormulaEvaluator.eval(input, attributeValues, allowed);
                return lookup(name, value, (List<Map<String, Object>>) table);
            }
            var formula = dv.get("formula") instanceof String s ? s : "0";
            var value = FormulaEvaluator.eval(formula, attributeValues, allowed);
            // B9 (DSA 5): Brueche werden aufgerundet (Anzeige + HP konsistent).
            return new SheetResponse.DerivedValueInfo(name, Math.ceil(value), null);
        } catch (FormulaEvaluator.EvaluationException e) {
            return new SheetResponse.DerivedValueInfo(name, 0, e.getMessage());
        }
    }

    private SheetResponse.DerivedValueInfo lookup(String name, double value,
                                                  List<Map<String, Object>> table) {
        List<Double> matches = new java.util.ArrayList<>();
        boolean invalidRange = false;
        boolean invalidRow = false;
        for (var row : table) {
            // Custom-Schema erlaubt Nicht-Zahlen (Audit final): als Fehlerzeile werten, nicht crashen.
            var minN = numberOrNull(row.get("min"));
            var maxN = numberOrNull(row.get("max"));
            var valueN = numberOrNull(row.get("value"));
            if ((row.containsKey("min") && minN == null)
                || (row.containsKey("max") && maxN == null)
                || (row.containsKey("value") && valueN == null)
                || (valueN == null && !row.containsKey("value"))) {
                invalidRow = true;
                continue;
            }
            double min = minN != null ? minN : Double.NEGATIVE_INFINITY;
            double max = maxN != null ? maxN : Double.POSITIVE_INFINITY;
            if (min > max) {
                invalidRange = true;
                continue;
            }
            if (value >= min && value <= max) {
                matches.add(valueN != null ? valueN : 0.0);
            }
        }
        if (matches.size() > 1) {
            return new SheetResponse.DerivedValueInfo(name, 0,
                "Overlapping table rows for value " + (long) value);
        }
        if (matches.size() == 1) {
            return new SheetResponse.DerivedValueInfo(name, matches.getFirst(), null);
        }
        // Audit P28: kaputte Zeilen (min > max) nicht als "Luecke" verkaufen.
        var reason = invalidRow
            ? "Invalid table row (non-numeric)"
            : invalidRange
                ? "Invalid table row (min > max)"
                : "No table row for value " + (long) value;
        return new SheetResponse.DerivedValueInfo(name, 0, reason);
    }

    private static Double numberOrNull(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    /** Tier-Suffixe werden ignoriert: "Zauberer II" erfüllt requiresTrait "Zauberer". */
    private static boolean hasTrait(List<String> selected, String name) {
        return selected.stream().anyMatch(s -> s.equals(name) || s.startsWith(name + " "));
    }
}
