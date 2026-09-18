package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Gemeinsame Utility-Methoden für Attribut-Extraktion, Grid-Distanz etc.
 * Vermeidet Code-Duplizierung über RollService, CombatService, IntentValidator.
 */
@Component
public class AttributeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AttributeUtils() {}

    /**
     * Extrahiert einen Integer-Wert aus {@code attributes_json}.
     * Namensvergleich case-insensitiv (ADR-014).
     */
    public static Optional<Integer> extractAttribute(GameEntity entity, String attrName) {
        try {
            var tree = objectMapper.readTree(entity.getAttributesJson());
            if (tree == null || !tree.isObject()) return Optional.empty();
            var direct = tree.get(attrName);
            if (direct != null && direct.isInt()) return Optional.of(direct.asInt());
            var fields = tree.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (com.lwe.core.util.RuleNames.eq(entry.getKey(), attrName)
                    && entry.getValue().isInt()) {
                    return Optional.of(entry.getValue().asInt());
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    /**
     * Manhattan-Distanz zwischen zwei Entity-Positionen (Grid-Tiles).
     * Leer, wenn Positionen fehlen oder unlesbar sind (Aufrufer entscheidet).
     */
    public static OptionalInt tryGridDistance(GameEntity a, GameEntity b) {
        try {
            var aTree = objectMapper.readTree(a.getPositionJson());
            var bTree = objectMapper.readTree(b.getPositionJson());
            if (!aTree.hasNonNull("x") || !aTree.hasNonNull("y")
                || !bTree.hasNonNull("x") || !bTree.hasNonNull("y")) return OptionalInt.empty();
            int ax = aTree.path("x").asInt(0), ay = aTree.path("y").asInt(0);
            int bx = bTree.path("x").asInt(0), by = bTree.path("y").asInt(0);
            return OptionalInt.of(Math.abs(ax - bx) + Math.abs(ay - by));
        } catch (Exception e) {
            return OptionalInt.empty();
        }
    }

    /** Manhattan-Distanz; 0 wenn keine Position vorhanden (Legacy-Verhalten). */
    public static int gridDistance(GameEntity a, GameEntity b) {
        return tryGridDistance(a, b).orElse(0);
    }
}
