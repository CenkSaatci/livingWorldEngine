package com.lwe.core.service;

import com.lwe.api.dto.ProbeResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wertet {@code conditionals} aus {@code rulesJson} für einen Character aus.
 * Liefert aktive Bedingungen + deren Auswirkung.
 */
@Service
public class ConditionEvaluator {

    public List<ProbeResponse.ConditionalResult> evaluate(
            List<Map<String, Object>> conditionals,
            Map<String, Integer> attributeValues) {

        if (conditionals == null) return List.of();

        return conditionals.stream()
            .map(c -> evaluateOne(c, attributeValues))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private ProbeResponse.ConditionalResult evaluateOne(
            Map<String, Object> c, Map<String, Integer> attrs) {

        var attr = (String) c.getOrDefault("attribute", "");
        var operator = (String) c.getOrDefault("operator", "gte");
        var value = ((Number) c.getOrDefault("value", 0)).intValue();
        var bonus = (String) c.getOrDefault("bonus", "");
        var target = (String) c.getOrDefault("target", "");

        var attrVal = attrs.getOrDefault(attr, 0);

        boolean active = switch (operator) {
            case "gt" -> attrVal > value;
            case "gte" -> attrVal >= value;
            case "lt" -> attrVal < value;
            case "lte" -> attrVal <= value;
            case "eq" -> attrVal == value;
            case "per_point" -> attrVal > value;
            default -> false;
        };

        if (!active) return null;

        // per_point: bonus × (attrVal − threshold)
        var effectiveBonus = bonus;
        if ("per_point".equals(operator)) {
            var points = attrVal - value;
            effectiveBonus = (points > 0 ? "+" : "") + points;
        }

        return new ProbeResponse.ConditionalResult(c.getOrDefault("name", "").toString(), effectiveBonus, target);
    }
}
