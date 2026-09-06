package com.lwe.core.service;

import com.lwe.api.dto.SheetResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wertet {@code derived_values} aus {@code rulesJson} aus.
 * Nutzt {@link FormulaEvaluator} mit Attribut-Werten als Variablen.
 */
@Service
public class DerivedValueService {

    public List<SheetResponse.DerivedValueInfo> evaluate(
            List<Map<String, Object>> derivedValues,
            Map<String, Integer> attributeValues) {

        if (derivedValues == null) return List.of();
        var allowed = attributeValues.keySet();

        return derivedValues.stream()
            .map(dv -> {
                var name = (String) dv.getOrDefault("name", "");
                var formula = (String) dv.getOrDefault("formula", "0");
                try {
                    var value = FormulaEvaluator.eval(formula, attributeValues, allowed);
                    return new SheetResponse.DerivedValueInfo(name, value);
                } catch (FormulaEvaluator.EvaluationException e) {
                    return new SheetResponse.DerivedValueInfo(name + " (Fehler)", 0);
                }
            })
            .collect(Collectors.toList());
    }
}
