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
                var input = (String) dv.getOrDefault("input", "0");
                var value = FormulaEvaluator.eval(input, attributeValues, allowed);
                return lookup(name, value, (List<Map<String, Object>>) table);
            }
            var formula = (String) dv.getOrDefault("formula", "0");
            var value = FormulaEvaluator.eval(formula, attributeValues, allowed);
            return new SheetResponse.DerivedValueInfo(name, value, null);
        } catch (FormulaEvaluator.EvaluationException e) {
            return new SheetResponse.DerivedValueInfo(name, 0, e.getMessage());
        }
    }

    private SheetResponse.DerivedValueInfo lookup(String name, double value,
                                                  List<Map<String, Object>> table) {
        for (var row : table) {
            var min = ((Number) row.getOrDefault("min", Double.NEGATIVE_INFINITY)).doubleValue();
            var max = ((Number) row.getOrDefault("max", Double.POSITIVE_INFINITY)).doubleValue();
            if (value >= min && value <= max) {
                var result = ((Number) row.getOrDefault("value", 0)).doubleValue();
                return new SheetResponse.DerivedValueInfo(name, result, null);
            }
        }
        return new SheetResponse.DerivedValueInfo(name, 0,
            "No table row for value " + (long) value);
    }

    /** Tier-Suffixe werden ignoriert: "Zauberer II" erfüllt requiresTrait "Zauberer". */
    private static boolean hasTrait(List<String> selected, String name) {
        return selected.stream().anyMatch(s -> s.equals(name) || s.startsWith(name + " "));
    }
}
