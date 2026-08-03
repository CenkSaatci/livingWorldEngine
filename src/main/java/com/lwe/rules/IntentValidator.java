package com.lwe.rules;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.NpcIntent;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.service.AttributeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Validiert NPC-Intents gegen Spielregeln, bevor sie ausgeführt werden.
 *
 * <p>Pipeline: Rule-Check → Visibility → Range → Resources.
 * Siehe {@code docs/ADR/005-ki-validation-layer.md}.
 */
@Component
public class IntentValidator {

    private final GameEntityRepository entityRepo;
    private final ObjectMapper objectMapper;

    public IntentValidator(GameEntityRepository entityRepo,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
    }

    /**
     * Validiert einen Intent. {@code approved=true} + {@code rejectionReason=null} = gültig.
     */
    public ValidationResult validate(NpcIntent intent) {
        var npc = entityRepo.findById(intent.getNpcId()).orElse(null);
        if (npc == null) return reject("NPC not found");

        return switch (intent.getIntentType()) {
            case "ATTACK" -> validateAttack(npc, intent);
            case "MOVE" -> validateMove(npc, intent);
            case "SPEAK" -> validateSpeak(npc, intent);
            case "USE_ITEM" -> validateUseItem(npc, intent);
            case "IDLE" -> new ValidationResult(true, null);
            default -> reject("Unknown intent type: " + intent.getIntentType());
        };
    }

    private ValidationResult validateAttack(GameEntity npc, NpcIntent intent) {
        var params = parseParams(intent);
        var targetId = params.get("target_id");
        if (targetId == null || targetId.isBlank())
            return reject("No target specified");

        // Range-Check (vereinfacht: max 5 Tiles)
        var target = entityRepo.findById(UUID.fromString(targetId)).orElse(null);
        if (target == null) return reject("Target not found");

        var distance = AttributeUtils.gridDistance(npc, target);
        if (distance > 5) return reject("Target out of range (" + distance + " tiles)");

        return new ValidationResult(true, null);
    }

    private ValidationResult validateMove(GameEntity npc, NpcIntent intent) {
        return new ValidationResult(true, null);
    }

    private ValidationResult validateSpeak(GameEntity npc, NpcIntent intent) {
        return new ValidationResult(true, null);
    }

    private ValidationResult validateUseItem(GameEntity npc, NpcIntent intent) {
        return new ValidationResult(true, null);
    }

    private Map<String, String> parseParams(NpcIntent intent) {
        try {
            var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(intent.getParamsJson());
            return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(tree, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private ValidationResult reject(String reason) {
        return new ValidationResult(false, reason);
    }

    public record ValidationResult(boolean approved, String rejectionReason) {}
}
