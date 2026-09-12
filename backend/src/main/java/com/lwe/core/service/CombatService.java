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
    private final com.lwe.core.util.EntityAccess entityAccess;
    private final DerivedValueService derivedValueService;
    private final EntityService entityService;
    private final CampaignMemberService campaignMemberService;
    private final Map<DiceExpressionParser.DiceSystem, RuleEngine> engines;
    private final ObjectMapper objectMapper;
    private final ConditionService conditionService;
    private final GameItemRepository itemRepo;

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
                         com.lwe.core.util.EntityAccess entityAccess,
                         java.util.List<RuleEngine> engineList,
                         ObjectMapper objectMapper,
                         RulesLoader rulesLoader,
                         CampaignMemberService campaignMemberService,
                         ConditionService conditionService,
                         GameItemRepository itemRepo,
                         DerivedValueService derivedValueService,
                         EntityService entityService) {
        this.objectMapper = objectMapper;
        this.conditionService = conditionService;
        this.itemRepo = itemRepo;
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
        this.entityAccess = entityAccess;
        this.derivedValueService = derivedValueService;
        this.entityService = entityService;
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
            var p = new CombatParticipant(
                session.getId(), entity.getId(), initiative, apMax, "A");
            // Playtest-Befund #8: Kampf-HP vom Entity (Startwert, z. B. LeP) statt Default 10.
            p.setHpMax(entity.getHpMax());
            p.setHpCurrent(Math.min(entity.getHpCurrent(), entity.getHpMax()));
            participants.add(p);
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
        entityAccess.requireControl(actorId, userId); // Runde 1: nur Kontrolleur/DM
        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var actor = findActor(participants, actorId);
        if (actor.getHpCurrent() <= 0)
            throw new CombatException("COMBAT_ACTOR_DEFEATED", "Actor is defeated");
        requireAp(actor);
        requireActionAllowed(session, actorId, actionType); // T3

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

        // Playtest: keine Angriffe auf bereits Besiegte (Heilung via Ability bleibt erlaubt).
        if (targetId != null) {
            var target = findActor(participants, targetId);
            if (target.getHpCurrent() <= 0)
                throw new CombatException("COMBAT_TARGET_DEFEATED", "Target is already defeated");
        }

        // P1: generisches Angriffswurf-Modell — nur wenn das System es konfiguriert
        // (dice_mechanics.combat.attack). Ohne Config bleibt das Verhalten wie bisher.
        var attackWorld = worldRepo.findById(session.getWorldId()).orElse(null);
        var attackCfg = isDamagingAction(attackWorld, session.getCampaignId(), actionType)
            ? resolveAttackConfig(attackWorld, session.getCampaignId())
            : null;
        if (attackCfg != null && targetId != null) {
            var hit = attackHits(userId, session, actorId, targetId, attackCfg, 0);
            if (Boolean.FALSE.equals(hit)) {
                deductAp(actor);
                sendCombatMessage(session.getWorldId(), "🎯 " + entityName(actorId)
                    + " verfehlt " + entityName(targetId));
                eventService.publish(session.getWorldId(), session.getCampaignId(),
                    COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
                        "actionType", "MISS", "damage", 0));
                return new CombatActionResult("MISS", 0, actor.getApCurrent(), false, null);
            }
        }

        var damage = rollDamage(userId, session.getWorldId(), actorId, actionType, session.getCampaignId());
        var damageType = resolveWeaponDamageType(actorId, itemId);
        damage = applyDamageModifiers(damage, targetId, damageType);
        deductAp(actor);

        // Death check
        if (targetId != null && damage > 0) {
            var target = findActor(participants, targetId);
            target.setHpCurrent(target.getHpCurrent() - damage);
            participantRepo.save(target);

            if (target.getHpCurrent() <= 0 && !tryAvoidDeath(userId, session, targetId, target)) {
                sendCombatMessage(session.getWorldId(), "💀 " + entityName(targetId) + " wurde besiegt!");
                eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED,
                    target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
            }
        }

        sendCombatMessage(session.getWorldId(), "⚔️ " + entityName(actorId) + " greift "
            + (targetId != null ? entityName(targetId) : "unbekannt") + " an: " + damage
            + " Schaden" + (damageType != null ? " (" + damageType + ")" : ""));
        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
            "actionType", actionType, "damage", damage));
        return new CombatActionResult(actionType, damage, actor.getApCurrent(), true, null);
    }

    @Transactional
    public CombatActionResult useAbility(UUID userId, UUID sessionId, UUID actorId,
                                          UUID abilityId, UUID targetId) {
        var session = validateSession(sessionId, userId, actorId);
        entityAccess.requireControl(actorId, userId); // Runde 1
        var participants = participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId);
        var actor = findActor(participants, actorId);
        if (actor.getHpCurrent() <= 0)
            throw new CombatException("COMBAT_ACTOR_DEFEATED", "Actor is defeated");

        var ability = abilityRepo.findById(abilityId)
            .orElseThrow(() -> new CombatException("ABILITY_NOT_FOUND", "Ability not found"));
        if (ability.getType() != Ability.AbilityType.ACTIVE)
            throw new CombatException("ABILITY_NOT_ACTIVE", "Ability is not an active ability");
        requireActionAllowed(session, actorId, "ABILITY"); // T3
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
                // Schadende Abilities treffen keine Besiegten (Heilung schon).
                if (target.getHpCurrent() <= 0)
                    throw new CombatException("COMBAT_TARGET_DEFEATED", "Target is already defeated");
                damage = applyDamageModifiers(damage, targetId, effects.damageType());
                target.setHpCurrent(Math.max(0, target.getHpCurrent() - damage));
                participantRepo.save(target);
                if (target.getHpCurrent() <= 0 && !tryAvoidDeath(userId, session, targetId, target)) {
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

    private record Effects(String damageExpr, String healExpr, String damageType) {}

    private Effects parseEffectsJson(String effectsJson) {
        if (effectsJson == null || effectsJson.isBlank()) return new Effects(null, null, null);
        try {
            var tree = new ObjectMapper().readTree(effectsJson);
            return new Effects(
                tree.path("damage").asText(null),
                tree.path("heal").asText(null),
                tree.path("damageType").asText(null));
        } catch (Exception e) {
            return new Effects(null, null, null);
        }
    }

    /** Waffen-Schadensart aus Item-Metadata (P23-T02); nur Items im Inventar des Actors. */
    private String resolveWeaponDamageType(UUID actorId, UUID itemId) {
        if (itemId == null) return null;
        var actor = entityRepo.findById(actorId).orElse(null);
        if (actor == null || !inventoryContains(actor, itemId)) return null;
        return itemRepo.findById(itemId)
            .map(i -> {
                if (i.getMetadataJson() == null || i.getMetadataJson().isBlank()) return null;
                try {
                    var t = objectMapper.readTree(i.getMetadataJson()).path("damage_type");
                    return t.isTextual() && !t.asText().isBlank() ? t.asText() : null;
                } catch (Exception e) {
                    return null;
                }
            })
            .orElse(null);
    }

    /** Ruestung (flat) + Resistenz/Vulnerabilitaet auf den Schaden (P23-T04/P29-T04).
     *  Reihenfolge: Ruestung abziehen, dann Typ-Multiplikator; resist+vulnerable heben sich auf. */
    private boolean inventoryContains(com.lwe.core.domain.GameEntity entity, UUID itemId) {
        var inv = entity.getInventoryJson();
        if (inv == null || inv.isBlank()) return false;
        try {
            var arr = objectMapper.readTree(inv);
            if (!arr.isArray()) return false;
            for (var n : arr) {
                if (itemId.toString().equals(n.path("itemId").asText(null))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private int applyDamageModifiers(int damage, UUID targetEntityId, String damageType) {
        if (damage <= 0 || targetEntityId == null) return damage;
        var target = entityRepo.findById(targetEntityId).orElse(null);
        if (target == null) return damage;
        int armor = 0;
        List<String> resistances = List.of();
        List<String> vulnerabilities = List.of();
        var meta = target.getMetadataJson();
        if (meta != null && !meta.isBlank()) {
            try {
                var node = objectMapper.readTree(meta);
                if (node.path("damage_armor").isNumber()) armor = node.path("damage_armor").asInt();
                resistances = stringList(node.path("damage_resistances"));
                vulnerabilities = stringList(node.path("damage_vulnerabilities"));
            } catch (Exception ignored) {}
        }
        int result = Math.max(0, damage - Math.max(0, armor));
        if (damageType != null) {
            var wanted = damageType.trim();
            boolean resistant = resistances.stream().anyMatch(r -> r != null && r.trim().equalsIgnoreCase(wanted));
            boolean vulnerable = vulnerabilities.stream().anyMatch(v -> v != null && v.trim().equalsIgnoreCase(wanted));
            if (vulnerable && !resistant) result *= 2;
            else if (resistant && !vulnerable) result /= 2;
        }
        return result;
    }

    private List<String> stringList(com.fasterxml.jackson.databind.JsonNode node) {
        if (!node.isArray()) return List.of();
        var out = new java.util.ArrayList<String>();
        for (var n : node) {
            if (n.isTextual()) out.add(n.asText());
        }
        return out;
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
        entityAccess.requireControl(actorId, userId); // Runde 1
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
        requireActionAllowed(session, actorId, "MANEUVER", "ACTION"); // T3
        checkRange("ACTION", targetId, actorId);

        // T2: angriffsbasierte Manöver (attackMalus) laufen durch dasselbe Gate wie Angriffe.
        var attackCfg = isDamagingAction(world, session.getCampaignId(), "ACTION")
            ? resolveAttackConfig(world, session.getCampaignId())
            : null;
        if (attackCfg != null && targetId != null) {
            int malus = def.get("attackMalus") instanceof Number n ? n.intValue() : 0;
            var hit = attackHits(userId, session, actorId, targetId, attackCfg, malus);
            if (Boolean.FALSE.equals(hit)) {
                actor.setApCurrent(actor.getApCurrent() - apCost);
                participantRepo.save(actor);
                sendCombatMessage(session.getWorldId(), "\u2694\ufe0f " + entityName(actorId) + " \u2013 "
                    + maneuverName + ": verfehlt " + entityName(targetId));
                eventService.publish(session.getWorldId(), session.getCampaignId(),
                    COMBAT_ACTION_EXECUTED, actorId, targetId, Map.of(
                        "actionType", "MISS", "maneuver", maneuverName, "damage", 0));
                return new CombatActionResult("MISS", 0, actor.getApCurrent(), false, null);
            }
        }

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
        String maneuverType = def.get("damageType") instanceof String dt ? dt : null;
        damage = applyDamageModifiers(damage, targetId, maneuverType);
        actor.setApCurrent(actor.getApCurrent() - apCost);
        participantRepo.save(actor);

        if (targetId != null && damage > 0) {
            var target = findActor(participants, targetId);
            target.setHpCurrent(target.getHpCurrent() - damage);
            participantRepo.save(target);
            if (target.getHpCurrent() <= 0 && !tryAvoidDeath(userId, session, targetId, target)) {
                eventService.publish(session.getWorldId(), session.getCampaignId(),
                    COMBAT_ACTION_EXECUTED, target.getEntityId(), null, Map.of("actionType", "DEFEATED"));
            }
        }

        sendCombatMessage(session.getWorldId(), "\u2694\ufe0f " + entityName(actorId) + " \u2013 "
            + maneuverName + ": " + damage + " Schaden"
            + (maneuverType != null ? " (" + maneuverType + ")" : ""));
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
        // Runde 1: Zug weitergeben darf der Kontrolleur des aktuellen Actors oder der DM.
        entityAccess.requireControl(session.getCurrentTurnEntityId(), userId);

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
        worldAccess.requireDm(session.getWorldId(), userId); // Runde 1: Beenden ist DM-only
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

    /** Playtest-Befund #10: aktive Session je Welt (Reload-/Deep-Link-Rehydrate). */
    public java.util.Optional<CombatSession> findActiveSession(UUID userId, UUID worldId) {
        requireWorldAccess(worldId, userId);
        return sessionRepo.findFirstByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "ACTIVE");
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
    // T4/Audit: Tod abwenden gibt Schicksalspunkte des ZIELS aus (auch durch den Angreifer
    // getriggert) — bewusste Regel-Entscheidung, explizite Reaktion bleibt SM-04.
    /** P1: attack-Config aus dem Regelwerk (optional, generisch).
     *  T2: Quelle ist genau eine von attribute (Engine-Modifikator), value
     *  (abgeleiteter Wert des Angreifers) oder skill (Per-Charakter-Fertigkeitswert). */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveAttackConfig(com.lwe.core.domain.World world, UUID campaignId) {
        if (world == null) return null;
        var rules = rulesLoader.loadRules(campaignId, world.getId());
        if (!(rules.get("dice_mechanics") instanceof Map<?, ?> dm)) return null;
        if (!(dm.get("combat") instanceof Map<?, ?> combat)) return null;
        if (!(combat.get("attack") instanceof Map<?, ?> attack)) return null;
        String source, sourceName;
        if (attack.get("attribute") instanceof String a) { source = "attribute"; sourceName = a; }
        else if (attack.get("value") instanceof String v) { source = "value"; sourceName = v; }
        else if (attack.get("skill") instanceof String s) { source = "skill"; sourceName = s; }
        else return null;
        var cfg = new java.util.HashMap<String, Object>();
        cfg.put("source", source);
        cfg.put("sourceName", sourceName);
        if (attack.get("target") instanceof String target) cfg.put("target", target);
        cfg.put("dice", attack.get("dice") instanceof String d ? d : "1d20");
        cfg.put("comparison", attack.get("comparison") instanceof String c ? c : "gte");
        return cfg;
    }

    /** null = kein Zielwert ableitbar (dann greift das Gate nicht).
     *  malus: positive Zahl erschwert den Angriff (Wuchtschlag etc.). */
    private Boolean attackHits(UUID userId, CombatSession session, UUID actorId, UUID targetId,
                               Map<String, Object> cfg, int malus) {
        var attacker = entityRepo.findById(actorId).orElse(null);
        var defender = entityRepo.findById(targetId).orElse(null);
        if (attacker == null || defender == null) return null;
        var targetName = (String) cfg.get("target");
        Integer targetValue = targetName == null ? null : derivedValue(defender, session, targetName);
        var comparison = (String) cfg.get("comparison");
        var dice = (String) cfg.get("dice");
        var source = (String) cfg.getOrDefault("source", "attribute");
        var sourceName = (String) cfg.get("sourceName");

        if ("attribute".equals(source)) {
            if (targetValue == null) return null;
            var attrValue = AttributeUtils.extractAttribute(attacker, sourceName).orElse(10);
            var world = worldRepo.findById(session.getWorldId()).orElse(null);
            var engine = resolveEngine(world, session.getCampaignId());
            var probe = engine.executeProbe(new RuleEngine.ProbeRequest(
                sourceName, attrValue, 0, targetValue, dice));
            // P1: Vergleichsrichtung kommt aus der Config (gte = D&D, lte = d100/CoC) —
            // damit sind Engine-Eigenheiten (Tier-Systeme) irrelevant.
            return "lte".equals(comparison)
                ? probe.total() + malus <= targetValue
                : probe.total() - malus >= targetValue;
        }

        // T2: value/skill sind bereits finale Werte — reiner Wurf, kein Engine-Modifikator.
        // lte = Wurf auf den eigenen Wert (DSA AT, CoC Fighting); target ist dann nur Doku.
        Integer base = "value".equals(source)
            ? derivedValue(attacker, session, sourceName)
            : skillValue(attacker, session, sourceName);
        if (base == null) return null;
        var roll = rollDice(dice);
        if ("lte".equals(comparison)) {
            return roll + malus <= base;
        }
        return targetValue != null && roll + base - malus >= targetValue;
    }

    private int rollDice(String expression) {
        var m = java.util.regex.Pattern.compile("(\\d+)d(\\d+)").matcher(expression == null ? "" : expression);
        if (!m.find()) return 0;
        int count = Integer.parseInt(m.group(1));
        int sides = Integer.parseInt(m.group(2));
        int total = 0;
        var rng = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) total += rng.nextInt(1, sides + 1);
        return total;
    }

    /** Per-Charakter-Fertigkeitswert (skillsJson), sonst Regel-Bonus; null = nicht vorhanden. */
    private Integer skillValue(GameEntity entity, CombatSession session, String name) {
        if (entity.getSkillsJson() != null && !entity.getSkillsJson().isBlank()) {
            try {
                var map = objectMapper.readValue(entity.getSkillsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Integer>>() {});
                if (map.containsKey(name)) return map.get(name);
            } catch (Exception e) {
                // fall through to rules
            }
        }
        var world = worldRepo.findById(session.getWorldId()).orElse(null);
        if (world == null) return null;
        var rules = rulesLoader.loadRules(session.getCampaignId(), world.getId());
        if (rules.get("skills") instanceof List<?> skills) {
            for (var s : skills) {
                if (s instanceof Map<?, ?> m && name.equals(m.get("name"))
                    && m.get("bonus") instanceof Number n) {
                    return n.intValue();
                }
            }
        }
        return null;
    }

    private Integer derivedValue(GameEntity entity, CombatSession session, String name) {
        var world = worldRepo.findById(session.getWorldId()).orElse(null);
        if (world == null) return null;
        var rules = rulesLoader.loadRules(session.getCampaignId(), world.getId());
        var raw = (java.util.List<java.util.Map<String, Object>>)
            rules.getOrDefault("derived_values", java.util.List.of());
        var attrs = parseAttributes(entity);
        if (attrs.isEmpty()) {
            attrs = new java.util.HashMap<>();
            if (rules.get("attributes") instanceof List<?> defs) {
                for (var def : defs) {
                    if (def instanceof Map<?, ?> m && m.get("name") instanceof String n) {
                        attrs.put(n, m.get("default") instanceof Number num ? num.intValue() : 10);
                    }
                }
            }
        }
        var traits = selectedTraits(entity);
        return derivedValueService.evaluate(raw, attrs, traits).stream()
            .filter(dv -> dv.name().equalsIgnoreCase(name))
            .filter(dv -> dv.error() == null) // Audit P1: kaputte Formel => Gate aus
            .map(dv -> (int) Math.round(dv.value()))
            .findFirst().orElse(null);
    }

    private java.util.List<String> selectedTraits(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return List.of();
        try {
            var node = objectMapper.readTree(entity.getMetadataJson()).path("traits");
            if (!node.isArray()) return List.of();
            var out = new java.util.ArrayList<String>();
            node.forEach(n -> { if (n.isTextual()) out.add(n.asText()); });
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private java.util.Map<String, Integer> parseAttributes(GameEntity entity) {
        if (entity.getAttributesJson() == null || entity.getAttributesJson().isBlank()) return java.util.Map.of();
        try {
            return objectMapper.readValue(entity.getAttributesJson(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }

    private void requireWorldAccess(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    /** T4: Tod abwenden — kostet fate.avoidDeathCost Schicksalspunkte, Ziel bleibt mit 1 HP. */
    private boolean tryAvoidDeath(UUID userId, CombatSession session, UUID targetId, CombatParticipant target) {
        var world = worldRepo.findById(session.getWorldId()).orElse(null);
        if (world == null) return false;
        var rules = rulesLoader.loadRules(session.getCampaignId(), world.getId());
        int cost = rules.get("fate") instanceof Map<?, ?> f && f.get("avoidDeathCost") instanceof Number n
            ? n.intValue() : 0;
        if (cost <= 0) return false;
        // Audit T7: atomar + wirft nie (sonst rollback-only trotz gefangenem Fehler).
        if (!entityService.spendFatePointsIfAvailable(targetId, userId, session.getCampaignId(), cost)) {
            return false;
        }
        target.setHpCurrent(1);
        participantRepo.save(target);
        sendCombatMessage(session.getWorldId(), "★ " + entityName(targetId) + " wendet den Tod ab!");
        eventService.publish(session.getWorldId(), session.getCampaignId(), COMBAT_ACTION_EXECUTED,
            targetId, null, Map.of("actionType", "FATE_AVOIDED_DEATH"));
        return true;
    }

    /** T3: aktive Zustände (rules.conditions[].blocks) können Aktionstypen sperren. */
    private void requireActionAllowed(CombatSession session, UUID actorId, String... actionTypes) {
        var entity = entityRepo.findById(actorId).orElse(null);
        if (entity == null) return;
        var rules = rulesLoader.loadRules(session.getCampaignId(), session.getWorldId());
        var blocked = conditionService.blockedActions(entity, rules);
        if (blocked.isEmpty()) return;
        for (var t : actionTypes) {
            if (t != null && blocked.contains(t.toUpperCase(java.util.Locale.ROOT))) {
                throw new CombatException("COMBAT_ACTION_BLOCKED",
                    "Action is blocked by an active condition");
            }
        }
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