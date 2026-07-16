package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import com.lwe.rules.DiceExpressionParser;
import com.lwe.rules.RuleEngine;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final AbilityRepository abilityRepo;
    private final SimpMessagingTemplate messaging;
    private final com.lwe.core.util.WorldAccess worldAccess;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CombatService(CombatSessionRepository sessionRepo,
                         CombatParticipantRepository participantRepo,
                         GameEntityRepository entityRepo,
                         WorldRepository worldRepo,
                         GameSystemRepository gameSystemRepo,
                         WorldEventService eventService,
                         RollService rollService,
                         AbilityRepository abilityRepo,
                         SimpMessagingTemplate messaging,
                         com.lwe.core.util.WorldAccess worldAccess,
                         java.util.List<RuleEngine> engineList) {
        this.sessionRepo = sessionRepo;
        this.participantRepo = participantRepo;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.eventService = eventService;
        this.rollService = rollService;
        this.abilityRepo = abilityRepo;
        this.messaging = messaging;
        this.worldAccess = worldAccess;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            this.engines.put(engine.getDiceSystem(), engine);
        }
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds) {
        return startCombat(userId, worldId, entityIds, null);
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds, UUID mapId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new CombatException("WORLD_ACCESS_DENIED", "Access denied");

        var entities = entityRepo.findAllById(entityIds);
        if (entities.size() < 2)
            throw new CombatException("COMBAT_INSUFFICIENT_PARTICIPANTS", "Need at least 2 participants");

        var engine = resolveEngine(world);

        var session = new CombatSession(worldId);
        if (mapId != null) session.setMapId(mapId);
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

        if ("MOVE".equals(actionType)) {
            deductAp(actor);
            sendCombatMessage(session.getWorldId(), "🚶 " + entityName(actorId) + " bewegt sich");
            eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED, actorId, null, Map.of(
                "actionType", "MOVE", "damage", 0));
            return new CombatActionResult("MOVE", 0, actor.getApCurrent(), true, null);
        }

        if ("DEFEND".equals(actionType)) {
            deductAp(actor);
            sendCombatMessage(session.getWorldId(), "🛡️ " + entityName(actorId) + " verteidigt sich");
            eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED, actorId, null, Map.of(
                "actionType", "DEFEND", "damage", 0));
            return new CombatActionResult("DEFEND", 0, actor.getApCurrent(), true, null);
        }

        checkRange(actionType, targetId, actorId);

        var damage = rollDamage(userId, session.getWorldId(), actorId, actionType);
        deductAp(actor);

        // Death check
        if (targetId != null && damage > 0) {
            var target = findActor(participants, targetId);
            target.setHpCurrent(target.getHpCurrent() - damage);
            participantRepo.save(target);

            if (target.getHpCurrent() <= 0) {
                sendCombatMessage(session.getWorldId(), "💀 " + entityName(targetId) + " wurde besiegt!");
                eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED,
                    target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
            }
        }

        sendCombatMessage(session.getWorldId(), "⚔️ " + entityName(actorId) + " greift "
            + (targetId != null ? entityName(targetId) : "unbekannt") + " an: " + damage + " Schaden");
        eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
            "actionType", actionType, "damage", damage));
        return new CombatActionResult(actionType, damage, actor.getApCurrent(), true, null);
    }

    @Transactional
    public CombatActionResult useAbility(UUID userId, UUID sessionId, UUID actorId,
                                          UUID abilityId, UUID targetId) {
        var session = validateSession(sessionId, userId, actorId);
        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var actor = findActor(participants, actorId);

        var ability = abilityRepo.findById(abilityId)
            .orElseThrow(() -> new CombatException("ABILITY_NOT_FOUND", "Ability not found"));
        if (ability.getType() != Ability.AbilityType.ACTIVE)
            throw new CombatException("ABILITY_NOT_ACTIVE", "Ability is not an active ability");
        if (actor.getApCurrent() < ability.getApCost())
            throw new CombatException("COMBAT_AP_INSUFFICIENT", "Not enough AP");

        // Parse damage from effects_json
        var effects = parseEffectsJson(ability.getEffectsJson());
        int damage = 0;
        if (effects.damageExpr != null) {
            var entity = entityRepo.findById(actorId)
                .orElseThrow(() -> new CombatException("ENTITY_NOT_FOUND", "Actor not found"));
            var world = worldRepo.findById(session.getWorldId())
                .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
            var rollResult = rollService.executeRoll(userId, session.getWorldId(), actorId,
                effects.damageExpr, 0, 0);
            damage = rollResult != null ? rollResult.total() : 0;
        }

        // Apply damage to target
        if (damage > 0 && targetId != null) {
            var target = participants.stream()
                .filter(p -> p.getEntityId().equals(targetId)).findFirst().orElse(null);
            if (target != null) {
                target.setHpCurrent(Math.max(0, target.getHpCurrent() - damage));
                participantRepo.save(target);
                if (target.getHpCurrent() <= 0) {
                    eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED,
                        target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
                }
            }
        }

        // Apply healing to actor
        if (effects.healExpr != null) {
            var healAmount = new com.lwe.rules.DiceExpression(effects.healExpr).getTotal();
            actor.setHpCurrent(Math.min(actor.getHpMax(), actor.getHpCurrent() + healAmount));
            participantRepo.save(actor);
        }

        actor.setApCurrent(actor.getApCurrent() - ability.getApCost());
        participantRepo.save(actor);

        eventService.publish(session.getWorldId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
            "actionType", "ABILITY_" + ability.getName(), "damage", damage));
        return new CombatActionResult("ABILITY_" + ability.getName(), damage,
            actor.getApCurrent(), true, null);
    }

    private record Effects(String damageExpr, String healExpr) {}

    private Effects parseEffectsJson(String effectsJson) {
        if (effectsJson == null || effectsJson.isBlank()) return new Effects(null, null);
        try {
            var tree = new ObjectMapper().readTree(effectsJson);
            return new Effects(
                tree.path("damage").asText(null),
                tree.path("heal").asText(null));
        } catch (Exception e) {
            return new Effects(null, null);
        }
    }

    private CombatSession validateSession(UUID sessionId, UUID userId, UUID actorId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        if (!"ACTIVE".equals(session.getStatus()))
            throw new CombatException("COMBAT_NOT_ACTIVE", "Combat has ended");
        worldAccess.requireAccess(session.getWorldId(), userId);
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
        var attacker = entityRepo.findById(actorId)
            .orElseThrow(() -> new CombatException("ENTITY_NOT_FOUND", "Actor not found"));
        var defender = entityRepo.findById(targetId)
            .orElseThrow(() -> new CombatException("ENTITY_NOT_FOUND", "Target not found"));
        var aPos = attacker.getPositionJson();
        var dPos = defender.getPositionJson();
        if (aPos == null || aPos.isBlank() || dPos == null || dPos.isBlank()) return;
        var range = AttributeUtils.gridDistance(attacker, defender);
        if (range > 5)
            throw new CombatException("COMBAT_RANGE_INVALID", "Target out of range (" + range + " tiles)");
    }

    private int rollDamage(UUID userId, UUID worldId, UUID actorId, String actionType) {
        if (!"ATTACK".equals(actionType)) return 0;
        var entity = entityRepo.findById(actorId)
            .orElseThrow(() -> new CombatException("ENTITY_NOT_FOUND", "Actor not found"));
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
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

    public CombatSession getSession(UUID userId, UUID sessionId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        requireOwnership(session.getWorldId(), userId);
        return session;
    }

    public List<Map<String, Object>> getParticipants(UUID sessionId) {
        return participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId).stream()
            .map(p -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", p.getId());
                m.put("entity_id", p.getEntityId());
                m.put("initiative", p.getInitiative());
                m.put("ap_current", p.getApCurrent());
                m.put("ap_max", p.getApMax());
                m.put("hp_current", p.getHpCurrent());
                m.put("hp_max", p.getHpMax());
                m.put("side", p.getSide());
                return m;
            })
            .toList();
    }

    // -- Helpers --

    private RuleEngine resolveEngine(World world) {
        if (world.getGameSystemId() != null) {
            try {
                var opt = gameSystemRepo.findById(world.getGameSystemId());
                if (opt.isPresent()) {
                    var gs = opt.get();
                    var system = DiceExpressionParser.detect(gs.getRulesJson());
                    var engine = engines.get(system);
                    if (engine != null) return engine;
                }
            } catch (IllegalArgumentException e) {
                // unsupported dice system → fall through to fallback
            }
        }
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }

    private int resolveApMax(World world, RuleEngine engine) {
        if (world.getGameSystemId() != null) {
            try {
                var opt = gameSystemRepo.findById(world.getGameSystemId());
                if (opt.isPresent()) {
                    var gs = opt.get();
                    var tree = objectMapper.readTree(gs.getRulesJson());
                    return tree.path("dice_mechanics").path("combat")
                        .path("action_points").path("max").asInt(2);
                }
            } catch (Exception ignored) {}
        }
        return 2;
    }

    private String resolveInitiativeAttr(GameEntity entity, World world) {
        return resolveCombatAttr(world, "initiative", "geschicklichkeit");
    }

    private String resolveDamageAttr(GameEntity entity, World world) {
        return resolveCombatAttr(world, "damage", "staerke");
    }

    private String resolveCombatAttr(World world, String combatKey, String fallback) {
        if (world.getGameSystemId() == null) return fallback;
        try {
            var gs = gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
            if (gs == null) return fallback;
            var tree = objectMapper.readTree(gs.getRulesJson());
            var expr = tree.path("dice_mechanics").path("combat").path(combatKey).asText("");
            var m = java.util.regex.Pattern.compile("[+-](\\w+)$").matcher(expr);
            return m.find() ? m.group(1) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void requireOwnership(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    private String entityName(UUID entityId) {
        return entityRepo.findById(entityId)
            .map(e -> e.getName() != null && !e.getName().isBlank() ? e.getName() : entityId.toString().substring(0, 8))
            .orElse(entityId.toString().substring(0, 8));
    }

    private void sendCombatMessage(UUID worldId, String text) {
        if (!isCombatChatEnabled(worldId)) return;
        var msg = Map.of(
            "sender", "⚔️ Combat",
            "text", text,
            "timestamp", java.time.Instant.now().toString());
        messaging.convertAndSend("/topic/world/" + worldId,
            Map.of("event_type", "CHAT_MESSAGE", "payload", msg));
    }

    private boolean isCombatChatEnabled(UUID worldId) {
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null) return true;
        try {
            var tree = new ObjectMapper().readTree(world.getSettingsJson());
            var val = tree.path("combat_chat_log");
            return val.isMissingNode() || val.asBoolean(true);
        } catch (Exception e) {
            return true;
        }
    }

    public record CombatActionResult(String actionType, int totalDamage,
                                     int apRemaining, boolean success, String error) {}

    public static class CombatException extends RuntimeException {
        private final String errorCode;
        public CombatException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}