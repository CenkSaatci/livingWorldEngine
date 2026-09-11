package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.ParticipantResponse;
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
    private final RulesLoader rulesLoader;
    private final CampaignMemberService campaignMemberService;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper;
    private final ConditionService conditionService;

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
                         java.util.List<RuleEngine> engineList,
                         ObjectMapper objectMapper,
                         RulesLoader rulesLoader,
                         CampaignMemberService campaignMemberService,
                         ConditionService conditionService) {
        this.objectMapper = objectMapper;
        this.conditionService = conditionService;
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
        this.rulesLoader = rulesLoader;
        this.campaignMemberService = campaignMemberService;
        this.engines = new EnumMap<>(DiceExpressionParser.DiceSystem.class);
        for (var engine : engineList) {
            this.engines.put(engine.getDiceSystem(), engine);
        }
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds) {
        return startCombat(userId, worldId, entityIds, null, null);
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds, UUID mapId) {
        return startCombat(userId, worldId, entityIds, mapId, null);
    }

    @Transactional
    public CombatSession startCombat(UUID userId, UUID worldId, List<UUID> entityIds, UUID mapId, UUID campaignId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)
            && (campaignId == null || !campaignMemberService.isDm(campaignId, userId)))
            throw new CombatException("WORLD_ACCESS_DENIED", "Access denied");

        var entities = entityRepo.findAllById(entityIds);
        if (entities.size() < 2)
            throw new CombatException("COMBAT_INSUFFICIENT_PARTICIPANTS", "Need at least 2 participants");

        var engine = resolveEngine(world, campaignId);

        var session = new CombatSession(worldId, campaignId);
        if (mapId != null) session.setMapId(mapId);
        session = sessionRepo.save(session);

        var participants = new ArrayList<CombatParticipant>();
        for (var entity : entities) {
            var initAttr = resolveInitiativeAttr(entity, world, campaignId);
            var attrValue = AttributeUtils.extractAttribute(entity, initAttr).orElse(10);
            var req = new RuleEngine.ProbeRequest(initAttr, attrValue, 0, 0);
            var initiative = engine.executeProbe(req).total();

            var apMax = resolveApMax(world, engine, campaignId);
            participants.add(new CombatParticipant(
                session.getId(), entity.getId(), initiative, apMax, "A"));
        }

        participants = (ArrayList<CombatParticipant>) participantRepo.saveAll(participants);
        participants.sort((a, b) -> Integer.compare(b.getInitiative(), a.getInitiative()));

        session.setCurrentTurnEntityId(participants.getFirst().getEntityId());
        session = sessionRepo.save(session);

        // Zustands-Tick auch fuer den Start-Actor (P29-Audit): Zugbeginn #1
        entityRepo.findById(participants.getFirst().getEntityId()).ifPresent(e -> {
            if (conditionService.active(e).stream().anyMatch(c -> c.rounds() != null)) {
                conditionService.tick(e);
                entityRepo.save(e);
            }
        });

        var entityIdsList = participants.stream().map(CombatParticipant::getEntityId).toList();
        eventService.publish(worldId, campaignId, COMBAT_STARTED, null, null, Map.of(
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
            eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED, actorId, null, Map.of(
                "actionType", "MOVE", "damage", 0));
            return new CombatActionResult("MOVE", 0, actor.getApCurrent(), true, null);
        }

        if ("DEFEND".equals(actionType)) {
            deductAp(actor);
            sendCombatMessage(session.getWorldId(), "🛡️ " + entityName(actorId) + " verteidigt sich");
            eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED, actorId, null, Map.of(
                "actionType", "DEFEND", "damage", 0));
            return new CombatActionResult("DEFEND", 0, actor.getApCurrent(), true, null);
        }

        checkRange(actionType, targetId, actorId);

        var damage = rollDamage(userId, session.getWorldId(), actorId, actionType, session.getCampaignId());
        deductAp(actor);

        // Death check
        if (targetId != null && damage > 0) {
            var target = findActor(participants, targetId);
            target.setHpCurrent(target.getHpCurrent() - damage);
            participantRepo.save(target);

            if (target.getHpCurrent() <= 0) {
                sendCombatMessage(session.getWorldId(), "💀 " + entityName(targetId) + " wurde besiegt!");
                eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED,
                    target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
            }
        }

        sendCombatMessage(session.getWorldId(), "⚔️ " + entityName(actorId) + " greift "
            + (targetId != null ? entityName(targetId) : "unbekannt") + " an: " + damage + " Schaden");
        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
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
                    eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED,
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

        actor.setApCurrent(Math.max(0, actor.getApCurrent() - ability.getApCost()));
        participantRepo.save(actor);

        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
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
        if (targetId == null) return;
        if (!"ATTACK".equals(actionType) && !"ACTION".equals(actionType)) return;
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

    private int rollDamage(UUID userId, UUID worldId, UUID actorId, String actionType, UUID campaignId) {
        var entity = entityRepo.findById(actorId)
            .orElseThrow(() -> new CombatException("ENTITY_NOT_FOUND", "Actor not found"));
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
        if (!isDamagingAction(world, campaignId, actionType)) return 0;
        var damageAttr = resolveDamageAttr(entity, world, campaignId);
        var rollResult = rollService.executeRoll(userId, worldId, actorId, damageAttr, 0, 0, campaignId);
        var base = rollResult != null ? rollResult.total() : 0;
        // Aktive Zustaende (P29-T01): Schadens-Modifikator
        var rules = rulesLoader.loadRules(campaignId, worldId);
        return Math.max(0, base + conditionService.modifier(entity, rules, "damage"));
    }

    private boolean isDamagingAction(World world, UUID campaignId, String actionType) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs != null) {
            try {
                var tree = objectMapper.readTree(gs.getRulesJson());
                var types = tree.path("dice_mechanics").path("combat").path("action_types");
                if (types.isArray() && !types.isEmpty()) {
                    for (var t : types) {
                        // Wizard schreibt Typen klein ("action"), UI sendet groß ("ACTION").
                        if (actionType.equalsIgnoreCase(t.asText())) return true;
                    }
                    return false;
                }
            } catch (Exception ignored) {}
        }
        return "ATTACK".equals(actionType) || "ACTION".equals(actionType);
    }

    private void deductAp(CombatParticipant actor) {
        actor.setApCurrent(Math.max(0, actor.getApCurrent() - 1));
        participantRepo.save(actor);
    }

    /** Kampfmanoever (P29-T03): Schadens-Effekte + AP-Kosten aus dem System.
     *  attackMalus ist dokumentiert, aber noch nicht angewandt (kein Attack-Roll-Modell). */
    @Transactional
    public CombatActionResult executeManeuver(UUID userId, UUID sessionId, UUID actorId,
                                              UUID targetId, String maneuverName) {
        var session = validateSession(sessionId, userId, actorId);
        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var actor = findActor(participants, actorId);
        var world = worldRepo.findById(session.getWorldId())
            .orElseThrow(() -> new CombatException("WORLD_NOT_FOUND", "World not found"));
        var rules = rulesLoader.loadRules(session.getCampaignId(), session.getWorldId());
        var def = findManeuver(rules, maneuverName)
            .orElseThrow(() -> new CombatException("COMBAT_MANEUVER_UNKNOWN",
                "Unknown maneuver: " + maneuverName));

        requireAp(actor);
        int apCost = def.get("apCost") instanceof Number n ? Math.max(1, n.intValue()) : 1;
        if (actor.getApCurrent() < apCost)
            throw new CombatException("COMBAT_AP_INSUFFICIENT", "Not enough AP");
        checkRange("ACTION", targetId, actorId);

        var base = rollDamage(userId, session.getWorldId(), actorId, "ACTION", session.getCampaignId());
        int bonus = 0;
        if (def.get("effects") instanceof List<?> effects) {
            for (var e : effects) {
                if (e instanceof Map<?, ?> m && "damage".equals(m.get("target"))
                    && "add".equals(m.get("op")) && m.get("value") instanceof Number n) {
                    bonus += n.intValue();
                }
            }
        }
        var damage = Math.max(0, base + bonus);
        actor.setApCurrent(actor.getApCurrent() - apCost);
        participantRepo.save(actor);

        if (targetId != null && damage > 0) {
            var target = findActor(participants, targetId);
            target.setHpCurrent(target.getHpCurrent() - damage);
            participantRepo.save(target);
            if (target.getHpCurrent() <= 0) {
                eventService.publish(session.getWorldId(), session.getCampaignId(),
                    COMBAT_ACTION_EXECUTED, target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
            }
        }

        sendCombatMessage(session.getWorldId(), "\u2694\ufe0f " + entityName(actorId) + " \u2013 "
            + maneuverName + ": " + damage + " Schaden");
        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED,
            actorId, targetId, Map.of("actionType", "MANEUVER", "maneuver", maneuverName, "damage", damage));
        return new CombatActionResult("MANEUVER:" + maneuverName, damage, actor.getApCurrent(), true, null);
    }

    @SuppressWarnings("unchecked")
    private java.util.Optional<Map<String, Object>> findManeuver(Map<String, Object> rules, String name) {
        if (!(rules.get("dice_mechanics") instanceof Map<?, ?> dm)) return java.util.Optional.empty();
        if (!(dm.get("combat") instanceof Map<?, ?> combat)) return java.util.Optional.empty();
        if (!(combat.get("maneuvers") instanceof List<?> list)) return java.util.Optional.empty();
        return list.stream()
            .filter(m -> m instanceof Map<?, ?> mm && name.equals(mm.get("name")))
            .map(m -> (Map<String, Object>) m)
            .findFirst();
    }

    @Transactional
    public CombatSession nextTurn(UUID userId, UUID sessionId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        requireWorldAccess(session.getWorldId(), userId);

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

        // Zustaende ticken (P29-T01): Runden zaehlen beim Zugbeginn des Actors
        entityRepo.findById(next.getEntityId()).ifPresent(e -> {
            if (conditionService.active(e).stream().anyMatch(c -> c.rounds() != null)) {
                conditionService.tick(e);
                entityRepo.save(e);
            }
        });

        session = sessionRepo.save(session);

        eventService.publish(session.getWorldId(), session.getCampaignId(), TURN_CHANGED, null, null, Map.of(
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
        requireWorldAccess(session.getWorldId(), userId);
        session.setStatus("ENDED");
        session.setEndedAt(java.time.Instant.now());
        session = sessionRepo.save(session);

        // BUG-8: Kampf-HP/AP auf die Entities zurückschreiben
        for (var p : participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId)) {
            entityRepo.findById(p.getEntityId()).ifPresent(e -> {
                e.setHpCurrent(Math.max(0, Math.min(p.getHpCurrent(), e.getHpMax())));
                e.setApCurrent(Math.max(0, Math.min(p.getApCurrent(), e.getApMax())));
                entityRepo.save(e);
            });
        }

        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ENDED, null, null,
            Map.of("sessionId", sessionId));
        return session;
    }

    public CombatSession getSession(UUID userId, UUID sessionId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new CombatException("COMBAT_NOT_FOUND", "Combat session not found"));
        requireWorldAccess(session.getWorldId(), userId);
        return session;
    }

    public List<ParticipantResponse> getParticipants(UUID sessionId) {
        return participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId).stream()
            .map(p -> {
                var name = entityRepo.findById(p.getEntityId())
                    .map(e -> e.getName())
                    .orElse(p.getEntityId().toString().substring(0, 8));
                return new ParticipantResponse(
                    p.getId(), p.getEntityId(), name, p.getInitiative(),
                    p.getApCurrent(), p.getApMax(), p.getHpCurrent(), p.getHpMax(), p.getSide());
            })
            .toList();
    }

    // -- Helpers --

    private RuleEngine resolveEngine(World world, UUID campaignId) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs != null) {
            try {
                var system = DiceExpressionParser.detect(gs.getRulesJson());
                var engine = engines.get(system);
                if (engine != null) return engine;
            } catch (IllegalArgumentException e) {
                // unsupported dice system → fall through to fallback
            }
        }
        return engines.getOrDefault(DiceExpressionParser.DiceSystem.D20,
            engines.values().iterator().next());
    }

    private int resolveApMax(World world, RuleEngine engine, UUID campaignId) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs != null) {
            try {
                var tree = objectMapper.readTree(gs.getRulesJson());
                return tree.path("dice_mechanics").path("combat")
                    .path("action_points").path("max").asInt(2);
            } catch (Exception ignored) {}
        }
        return 2;
    }

    private String resolveInitiativeAttr(GameEntity entity, World world, UUID campaignId) {
        return resolveCombatAttr(world, campaignId, "initiative", "geschicklichkeit");
    }

    private String resolveDamageAttr(GameEntity entity, World world, UUID campaignId) {
        return resolveCombatAttr(world, campaignId, "damage", "staerke");
    }

    private String resolveCombatAttr(World world, UUID campaignId, String combatKey, String fallback) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(world);
        if (gs == null) return fallback;
        try {
            var tree = objectMapper.readTree(gs.getRulesJson());
            var expr = tree.path("dice_mechanics").path("combat").path(combatKey).asText("");
            var m = java.util.regex.Pattern.compile("[+-](\\w+)$").matcher(expr);
            return m.find() ? m.group(1) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    // Bewusst: Turn-Wechsel/Lesen/Ende braucht nur Welt-Mitgliedschaft (kein Owner),
    // Aktionen zusätzlich den aktuellen Turn (validateSession). Siehe Finding F-Combat-Auth.
    private void requireWorldAccess(UUID worldId, UUID userId) {
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