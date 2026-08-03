package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.NpcIntent;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.service.FactionService;
import com.lwe.core.service.WorldEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.lwe.core.service.WorldEventService.EventType.*;

/**
 * Führt einen approved NPC-Intent tatsächlich in der Spielwelt aus.
 * Wird von NpcIntentService nach Approve aufgerufen (sowohl autonom als auch DM-approved).
 *
 * <p>Dispatch-Tabelle:
 * <ul>
 *   <li>{@code MOVE} → Position in {@code entities.positionJson} updaten</li>
 *   <li>{@code ATTACK} → Schaden würfeln + World-Event</li>
 *   <li>{@code SPEAK} → Chat-ähnliches World-Event</li>
 *   <li>{@code USE_ITEM} → (vorbereitet, folgt in späterer Phase)</li>
 *   <li>{@code IDLE} → No-op</li>
 * </ul>
 */
@Component
public class IntentExecutor {

    private static final Logger log = LoggerFactory.getLogger(IntentExecutor.class);

    private final GameEntityRepository entityRepo;
    private final WorldEventService eventService;
    private final FactionService factionService;
    private final ObjectMapper objectMapper;

    public IntentExecutor(GameEntityRepository entityRepo, WorldEventService eventService,
                          FactionService factionService,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.eventService = eventService;
        this.factionService = factionService;
    }

    /**
     * Zentrale Dispatch-Methode. Wird nach erfolgreicher Validierung + Approve aufgerufen.
     */
    public void execute(NpcIntent intent) {
        try {
            switch (intent.getIntentType()) {
                case "MOVE" -> executeMove(intent);
                case "ATTACK" -> executeAttack(intent);
                case "SPEAK" -> executeSpeak(intent);
                case "USE_ITEM" -> executeUseItem(intent);
                case "CHANGE_RELATION" -> executeChangeRelation(intent);
                case "IDLE" -> {} // no-op
                default -> log.warn("Unknown intent type: {}", intent.getIntentType());
            }
        } catch (Exception e) {
            log.error("Failed to execute intent {}: {}", intent.getId(), e.getMessage());
        }
    }

    private void executeMove(NpcIntent intent) {
        var npc = entityRepo.findById(intent.getNpcId()).orElse(null);
        if (npc == null) return;

        var params = parseParams(intent);
        var x = params.get("x");
        var y = params.get("y");
        if (x == null || y == null) return;

        try {
            var pos = Map.of("x", Integer.parseInt(x), "y", Integer.parseInt(y));
            npc.setPositionJson(objectMapper.writeValueAsString(pos));
            entityRepo.save(npc);

            eventService.publish(intent.getWorldId(), ENTITY_MOVED,
                intent.getNpcId(), null, Map.of(
                    "entityId", intent.getNpcId(),
                    "x", x, "y", y,
                    "reasoning", intent.getReasoning()
                ));
            log.info("NPC {} moved to ({}, {})", npc.getName(), x, y);
        } catch (Exception e) {
            log.warn("Failed to move NPC {}: {}", npc.getId(), e.getMessage());
        }
    }

    private void executeAttack(NpcIntent intent) {
        var npc = entityRepo.findById(intent.getNpcId()).orElse(null);
        if (npc == null) return;

        // Schaden würfeln (vereinfacht: 1d6 Basis-Schaden ohne Attribut)
        var damage = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 7);

        eventService.publish(intent.getWorldId(), COMBAT_ACTION_EXECUTED,
            intent.getNpcId(), null, Map.of(
                "actionType", "ATTACK",
                "damage", damage,
                "reasoning", intent.getReasoning()
            ));
        log.info("NPC {} attacks for {} damage", npc.getName(), damage);
    }

    private void executeSpeak(NpcIntent intent) {
        var npc = entityRepo.findById(intent.getNpcId()).orElse(null);
        var name = npc != null ? npc.getName() : "Unknown NPC";

        eventService.publish(intent.getWorldId(), CHAT_MESSAGE,
            intent.getNpcId(), null, Map.of(
                "sender", name,
                "text", intent.getReasoning()
            ));
        log.info("NPC {} speaks: {}", name, intent.getReasoning());
    }

    private void executeUseItem(NpcIntent intent) {
        // Placeholder — Item-Nutzung folgt in späterer Phase
        log.info("NPC {} would use item (not implemented)", intent.getNpcId());
    }

    private void executeChangeRelation(NpcIntent intent) {
        var params = parseParams(intent);
        var factionA = params.get("faction_a_id");
        var factionB = params.get("faction_b_id");
        var newStatus = params.get("relation_status");
        if (factionA == null || factionB == null || newStatus == null) return;

        try {
            // System-interner Aufruf ohne User-Check (Intent wurde bereits validiert)
            factionService.setRelationInternal(
                UUID.fromString(factionA),
                UUID.fromString(factionB),
                newStatus
            );
            log.info("Faction relation changed: {} ↔ {} = {}", factionA, factionB, newStatus);
        } catch (Exception e) {
            log.warn("Failed to change faction relation: {}", e.getMessage());
        }
    }

    private Map<String, String> parseParams(NpcIntent intent) {
        try {
            var paramsJson = intent.getParamsJson();
            if (paramsJson == null || paramsJson.isBlank()) return Map.of();
            var tree = objectMapper.readTree(paramsJson);
            return objectMapper.convertValue(tree, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
