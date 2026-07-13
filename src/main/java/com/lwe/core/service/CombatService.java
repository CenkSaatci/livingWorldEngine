package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import com.lwe.rules.DiceExpressionParser;
import com.lwe.rules.RuleEngine;
import org.springframework.stereotype.Service;
import static com.lwe.core.service.WorldEventService.EventType.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CombatService {

    private final CombatSessionRepository sessionRepo;
    private final CombatParticipantRepository participantRepo;
    private final GameEntityRepository entityRepo;
    private final WorldRepository worldRepo;
    private final GameSystemRepository gameSystemRepo;
    private final WorldEventService eventService;
    private final RollService rollService;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CombatService(CombatSessionRepository sessionRepo,
                         CombatParticipantRepository participantRepo,
                         GameEntityRepository entityRepo,
                         WorldRepository worldRepo,
                         GameSystemRepository gameSystemRepo,
                         WorldEventService eventService,
                         RollService rollService,
                         java.util.List<RuleEngine> engineList) {
        this.sessionRepo = sessionRepo;
        this.participantRepo = participantRepo;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.eventService = eventService;
        this.rollService = rollService;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            var system = engine.getClass().getSimpleName().toLowerCase().contains("pool")
                ? DiceExpressionParser.DiceSystem.POOL
                : DiceExpressionParser.DiceSystem.D20;
            this.engines.put(system, engine);
        }
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new CombatException("WORLD_ACCESS_DENIED", "Access denied");

        var entities = entityRepo.findAllById(entityIds);
        if (entities.size() < 2)
            throw new CombatException("COMBAT_INSUFFICIENT_PARTICIPANTS", "Need at least 2 participants");

        var engine = resolveEngine(world);

        var session = new CombatSession(worldId);
        session = sessionRepo.save(session);

        var participants = new ArrayList<CombatParticipant>();
        for (var entity : entities) {
            var initAttr = resolveInitiativeAttr(entity, world);
            var attrValue = AttributeUtils.extractAttribute(entity, initAttr).orElse(10);
            var req = new RuleEngine.ProbeRequest(initAttr, attrValue, 0, 0);
            var initiative = engine.executeProbe(req).total();

            var apMax = resolveApMax(world, engine);
            participants.add(new CombatParticipant(
                session.getId(), entity.getId(), initiative, apMax, "A"));
        }

        participants = (ArrayList<CombatParticipant>) participantRepo.saveAll(participants);
        participants.sort((a, b) -> Integer.compare(b.getInitiative(), a.getInitiative()));

        session.setCurrentTurnEntityId(participants.getFirst().getEntityId());
        session = sessionRepo.save(session);

        var entityIdsList = participants.stream().map(CombatParticipant::getEntityId).toList();
        eventService.publish(worldId, COMBAT_STARTED, null, null, Map.of(
            "sessionId", session.getId(),
            "participants", entityIdsList,
            "firstTurn", participants.getFirst().getEntityId()
        ));
        return session;
    }

    @Transactional
    public CombatActionResult executeAction(UUID userId, UUID sessionId, UUID actorId,
                                            String actionType, UUID targetId, UUID itemId) {
        var session = validateSession(sessionId, userId, actorId);
        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var actor = findActor(participants, actorId);
        requireAp(actor);

        checkRange(actionType, targetId, actorId);

        var damage = rollDamage(userId, session.getWorldId(), actorId, actionType);
        deductAp(actor);

        eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
            "actionType", actionType, "damage", damage));
        return new CombatActionResult(actionType, damage, actor.getApCurrent(), true, null);
    }

    private CombatSession validateSession(UUID sessionId, UUID userId, UUID actorId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        if (!"ACTIVE".equals(session.getStatus()))
            throw new CombatException("COMBAT_NOT_ACTIVE", "Combat has ended");
        if (!userId.equals(worldRepo.findById(session.getWorldId()).orElseThrow().getOwnerId()))
            throw new CombatException("WORLD_ACCESS_DENIED", "Access denied");
        if (!actorId.equals(session.getCurrentTurnEntityId()))
            throw new CombatException("COMBAT_NOT_YOUR_TURN", "Not your turn");
        return session;
    }

    private CombatParticipant findActor(List<CombatParticipant> participants, UUID actorId) {
        return participants.stream()
            .filter(p -> p.getEntityId().equals(actorId))
            .findFirst()
            .orElseThrow(() -> new CombatException("COMBAT_TARGET_INVALID", "Actor not in combat"));
    }

    private void requireAp(CombatParticipant actor) {
        if (actor.getApCurrent() < 1)
            throw new CombatException("COMBAT_AP_INSUFFICIENT", "Not enough AP");
    }

    private void checkRange(String actionType, UUID targetId, UUID actorId) {
        if (!"ATTACK".equals(actionType) || targetId == null) return;
        var attacker = entityRepo.findById(actorId).orElse(null);
        var defender = entityRepo.findById(targetId).orElse(null);
        if (attacker == null || defender == null) return;
        var aPos = attacker.getPositionJson();
        var dPos = defender.getPositionJson();
        if (aPos == null || aPos.isBlank() || dPos == null || dPos.isBlank()) return;
        var range = AttributeUtils.gridDistance(attacker, defender);
        if (range > 5)
            throw new CombatException("COMBAT_RANGE_INVALID", "Target out of range (" + range + " tiles)");
    }

    private int rollDamage(UUID userId, UUID worldId, UUID actorId, String actionType) {
        if (!"ATTACK".equals(actionType)) return 0;
        var entity = entityRepo.findById(actorId).orElse(null);
        if (entity == null) return 0;
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null) return 0;
        var damageAttr = resolveDamageAttr(entity, world);
        var rollResult = rollService.executeRoll(userId, worldId, actorId, damageAttr, 0, 0);
        return rollResult != null ? rollResult.total() : 0;
    }

    private void deductAp(CombatParticipant actor) {
        actor.setApCurrent(actor.getApCurrent() - 1);
        participantRepo.save(actor);
    }

    @Transactional
    public CombatSession nextTurn(UUID userId, UUID sessionId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        requireOwnership(session.getWorldId(), userId);

        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var currentIdx = -1;
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getEntityId().equals(session.getCurrentTurnEntityId())) {
                currentIdx = i;
                break;
            }
        }

        int nextIdx = (currentIdx + 1) % participants.size();
        session.setCurrentTurnEntityId(participants.get(nextIdx).getEntityId());
        if (nextIdx == 0) session.setRound(session.getRound() + 1);

        // Refresh AP for next turn's actor
        var next = participants.get(nextIdx);
        next.setApCurrent(next.getApMax());
        participantRepo.save(next);

        session = sessionRepo.save(session);

        eventService.publish(session.getWorldId(), TURN_CHANGED, null, null, Map.of(
            "sessionId", sessionId,
            "currentTurn", session.getCurrentTurnEntityId(),
            "round", session.getRound()
        ));
        return session;
    }

    @Transactional
    public CombatSession endCombat(UUID userId, UUID sessionId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        requireOwnership(session.getWorldId(), userId);
        session.setStatus("ENDED");
        session.setEndedAt(java.time.Instant.now());
        session = sessionRepo.save(session);

        eventService.publish(session.getWorldId(), COMBAT_ENDED, null, null,
            Map.of("sessionId", sessionId));
        return session;
    }

    // -- Helpers --

    private RuleEngine resolveEngine(World world) {
        if (world.getGameSystemId() != null) {
            var gs = gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
            if (gs != null) {
                var system = DiceExpressionParser.detect(gs.getRulesJson());
                var engine = engines.get(system);
                if (engine != null) return engine;
            }
        }
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }

    private int resolveApMax(World world, RuleEngine engine) {
        if (world.getGameSystemId() != null) {
            try {
                var gs = gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
                if (gs != null) {
                    var tree = objectMapper.readTree(gs.getRulesJson());
                    return tree.path("dice_mechanics").path("combat")
                        .path("action_points").path("max").asInt(2);
                }
            } catch (Exception ignored) {}
        }
        return 2;
    }

    private String resolveInitiativeAttr(GameEntity entity, World world) {
        return "geschicklichkeit"; // Default — P2-T07 wird Parsing aus rules_json ergänzen
    }

    private String resolveDamageAttr(GameEntity entity, World world) {
        return "staerke"; // Default
    }

    private void requireOwnership(UUID worldId, UUID userId) {
        worldRepo.findById(worldId).ifPresent(w -> {
            if (!w.getOwnerId().equals(userId))
                throw new CombatException("WORLD_ACCESS_DENIED", "Access denied");
        });
    }

    public record CombatActionResult(String actionType, int totalDamage,
                                     int apRemaining, boolean success, String error) {}

    public static class CombatException extends RuntimeException {
        private final String errorCode;
        public CombatException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}