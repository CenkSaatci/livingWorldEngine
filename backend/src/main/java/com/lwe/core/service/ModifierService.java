package com.lwe.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wendet {@code modifierFormula} aus {@code rulesJson} auf Attribute an.
 * Beispiel D&D: {@code floor((@{name}-10)/2)} für jedes Attribut.
 */
@Service
public class ModifierService {

    private static final Logger log = LoggerFactory.getLogger(ModifierService.class);

    /** Bare-Platzhalter nur als ganzes Wort ersetzen (kein Teilstring-Treffer in "nickname"). */
    private static final Pattern BARE_NAME = Pattern.compile("\\bname\\b");

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
            var value = String.valueOf(entry.getValue());
            var expr = modifierFormula.replace("@{name}", value).replace("@name", value);
            expr = BARE_NAME.matcher(expr).replaceAll(value);
            try {
                var evaluator = new FormulaEvaluator(expr, allowed);
                result.put(entry.getKey(), evaluator.evaluate(attributeValues));
            } catch (FormulaEvaluator.EvaluationException e) {
                // Sichtbar machen statt still 0: Regel-Fehler bleibt am Sheet erkennbar.
                log.warn("modifierFormula '{}' fuer Attribut '{}' nicht auswertbar: {}",
                    modifierFormula, entry.getKey(), e.getMessage());
                result.put(entry.getKey(), 0.0);
            }
        }
        return result;
    }
}
