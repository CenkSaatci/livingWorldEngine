package com.lwe.core.service;

import com.lwe.core.domain.NpcIntent;
import com.lwe.core.repository.NpcIntentRepository;
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

    public NpcIntentService(NpcIntentRepository repo, IntentValidator validator,
                            WorldEventService eventService) {
        this.repo = repo;
        this.validator = validator;
        this.eventService = eventService;
    }

    @Transactional
    public NpcIntent create(UUID worldId, UUID npcId, String intentType,
                            String paramsJson, String reasoning) {
        var intent = new NpcIntent(worldId, npcId, intentType, paramsJson, reasoning);

        // Automatisch validieren
        var result = validator.validate(intent);
        if (result.approved()) {
            intent.setStatus("pending");
        } else {
            intent.setStatus("rejected");
            intent.setRejectionReason(result.rejectionReason());
        }

        intent = repo.save(intent);

        eventService.publish(worldId, WorldEventService.EventType.NPC_INTENT_PROPOSED,
            npcId, null, java.util.Map.of(
                "intentId", intent.getId(),
                "intentType", intentType,
                "status", intent.getStatus()
            ));

        return intent;
    }

    public List<NpcIntent> listPending(UUID worldId) {
        return repo.findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "pending");
    }

    @Transactional
    public NpcIntent approve(UUID intentId) {
        var intent = repo.findById(intentId)
            .orElseThrow(() -> new IntentException("INTENT_NOT_FOUND", "Intent not found"));
        intent.setStatus("approved");
        intent.setValidatedAt(Instant.now());
        intent = repo.save(intent);

        eventService.publish(intent.getWorldId(), WorldEventService.EventType.NPC_INTENT_APPROVED,
            intent.getNpcId(), null, java.util.Map.of("intentId", intentId));
        return intent;
    }

    @Transactional
    public NpcIntent reject(UUID intentId, String reason) {
        var intent = repo.findById(intentId)
            .orElseThrow(() -> new IntentException("INTENT_NOT_FOUND", "Intent not found"));
        intent.setStatus("rejected");
        intent.setRejectionReason(reason);
        intent.setValidatedAt(Instant.now());
        intent = repo.save(intent);

        eventService.publish(intent.getWorldId(), WorldEventService.EventType.NPC_INTENT_REJECTED,
            intent.getNpcId(), null, java.util.Map.of("intentId", intentId, "reason", reason));
        return intent;
    }

    public static class IntentException extends RuntimeException {
        private final String errorCode;
        public IntentException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
