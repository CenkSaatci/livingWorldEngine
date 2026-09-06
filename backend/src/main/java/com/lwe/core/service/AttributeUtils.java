package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
     */
    public static Optional<Integer> extractAttribute(GameEntity entity, String attrName) {
        try {
            var tree = objectMapper.readTree(entity.getAttributesJson());
            var node = tree.get(attrName);
            if (node != null && node.isInt()) return Optional.of(node.asInt());
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    /**
     * Manhattan-Distanz zwischen zwei Entity-Positionen (Grid-Tiles).
     */
    public static int gridDistance(GameEntity a, GameEntity b) {
        try {
            var aTree = objectMapper.readTree(a.getPositionJson());
            var bTree = objectMapper.readTree(b.getPositionJson());
            int ax = aTree.path("x").asInt(0), ay = aTree.path("y").asInt(0);
            int bx = bTree.path("x").asInt(0), by = bTree.path("y").asInt(0);
            return Math.abs(ax - bx) + Math.abs(ay - by);
        } catch (Exception e) {
            return 0; // Keine Position = nah genug
        }
    }
}
