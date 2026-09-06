package com.lwe.core.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wendet {@code modifierFormula} aus {@code rulesJson} auf Attribute an.
 * Beispiel D&D: {@code floor((@{name}-10)/2)} für jedes Attribut.
 */
@Service
public class ModifierService {

    /**
     * Berechnet Modifier für alle Attribute.
     *
     * @param modifierFormula Formel, z.B. "floor((@{name}-10)/2)". {@code @{name}} wird durch den Attribut-Namen ersetzt.
     * @param attributeValues Attribut-Name → Wert
     * @return Attribut-Name → Modifier
     */
    public Map<String, Double> calculateModifiers(String modifierFormula, Map<String, Integer> attributeValues) {
        if (modifierFormula == null || modifierFormula.isBlank()) {
            return attributeValues.keySet().stream().collect(Collectors.toMap(k -> k, k -> 0.0));
        }
        var result = new HashMap<String, Double>();
        var allowed = attributeValues.keySet();
        for (var entry : attributeValues.entrySet()) {
            // Ersetze @{name} durch den tatsächlichen Wert für diese Berechnung
            var expr = modifierFormula.replace("@{name}", String.valueOf(entry.getValue()))
                .replace("@name", String.valueOf(entry.getValue()))
                .replace("name", String.valueOf(entry.getValue()));
            try {
                var evaluator = new FormulaEvaluator(expr, allowed);
                result.put(entry.getKey(), evaluator.evaluate(attributeValues));
            } catch (FormulaEvaluator.EvaluationException e) {
                result.put(entry.getKey(), 0.0);
            }
        }
        return result;
    }
}
