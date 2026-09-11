package com.lwe.core.service;

import com.lwe.core.domain.NpcIntent;
import com.lwe.core.repository.NpcIntentRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.IntentExecutor;
import com.lwe.rules.IntentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NpcIntentService {

    private final NpcIntentRepository repo;
    private final IntentValidator validator;
    private final WorldEventService eventService;
    private final WorldRepository worldRepo;
    private final IntentExecutor executor;
    private final WorldAccess worldAccess;

    public NpcIntentService(NpcIntentRepository repo, IntentValidator validator,
                            WorldEventService eventService, WorldRepository worldRepo,
                            IntentExecutor executor, WorldAccess worldAccess) {
        this.repo = repo;
        this.validator = validator;
        this.eventService = eventService;
        this.worldRepo = worldRepo;
        this.executor = executor;
        this.worldAccess = worldAccess;
    }

    @Transactional
    public NpcIntent create(UUID worldId, UUID npcId, String intentType,
                            String paramsJson, String reasoning) {
        var intent = new NpcIntent(worldId, npcId, intentType, paramsJson, reasoning);

        // 1. Validieren
        var result = validator.validate(intent);
        if (!result.approved()) {
            intent.setStatus("rejected");
            intent.setRejectionReason(result.rejectionReason());
            intent = repo.save(intent);
            eventService.publish(worldId, WorldEventService.EventType.NPC_INTENT_PROPOSED,
                npcId, null, java.util.Map.of(
                    "intentId", intent.getId(),
                    "intentType", intentType,
                    "status", intent.getStatus()
                ));
            return intent;
        }

        // 2. ai_mode aus worlds.settings_json auslesen
        var aiMode = readAiMode(worldId);

        if ("autonom".equals(aiMode)) {
            // Autonom: sofort approven + ausführen
            intent.setStatus("approved");
            intent.setValidatedAt(Instant.now());
            intent = repo.save(intent);

            eventService.publish(worldId, WorldEventService.EventType.NPC_INTENT_PROPOSED,
                npcId, null, java.util.Map.of(
                    "intentId", intent.getId(),
                    "intentType", intentType,
                    "status", "approved"
                ));

            executor.execute(intent);
        } else if ("suggest".equals(aiMode) || aiMode == null) {
            // Suggest: pending, DM muss freigeben
            intent.setStatus("pending");
            intent = repo.save(intent);

            eventService.publish(worldId, WorldEventService.EventType.NPC_INTENT_PROPOSED,
                npcId, null, java.util.Map.of(
                    "intentId", intent.getId(),
                    "intentType", intentType,
                    "status", "pending"
                ));
        } else {
            // "off" oder unbekannt: ablehnen
            intent.setStatus("rejected");
            intent.setRejectionReason("AI mode is off");
            intent = repo.save(intent);
        }

        return intent;
    }

    /** P27-T06: Queue ist DM-only (Welt-DM); der Bot legt Intents nur an. */
    public List<NpcIntent> listPending(UUID worldId, UUID userId) {
        worldAccess.requireDm(worldId, userId);
        return repo.findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "pending");
    }

    @Transactional
    public NpcIntent approve(UUID intentId, UUID userId) {
        var intent = repo.findById(intentId)
            .orElseThrow(() -> new IntentException("INTENT_NOT_FOUND", "Intent not found"));
        worldAccess.requireDm(intent.getWorldId(), userId);
        intent.setStatus("approved");
        intent.setValidatedAt(Instant.now());
        intent = repo.save(intent);

        eventService.publish(intent.getWorldId(), WorldEventService.EventType.NPC_INTENT_APPROVED,
            intent.getNpcId(), null, java.util.Map.of("intentId", intentId));

        // Intent ausführen
        executor.execute(intent);

        return intent;
    }

    @Transactional
    public NpcIntent reject(UUID intentId, String reason, UUID userId) {
        var intent = repo.findById(intentId)
            .orElseThrow(() -> new IntentException("INTENT_NOT_FOUND", "Intent not found"));
        worldAccess.requireDm(intent.getWorldId(), userId);
        intent.setStatus("rejected");
        intent.setRejectionReason(reason);
        intent.setValidatedAt(Instant.now());
        intent = repo.save(intent);

        eventService.publish(intent.getWorldId(), WorldEventService.EventType.NPC_INTENT_REJECTED,
            intent.getNpcId(), null, java.util.Map.of("intentId", intentId, "reason", reason));
        return intent;
    }

    private String readAiMode(UUID worldId) {
        return worldRepo.findById(worldId)
            .map(w -> {
                try {
                    var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(w.getSettingsJson());
                    return tree.path("ai_mode").asText(null);
                } catch (Exception e) {
                    return null;
                }
            })
            .orElse(null);
    }

    public static class IntentException extends RuntimeException {
        private final String errorCode;
        public IntentException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
