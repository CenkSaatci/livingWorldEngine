package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.*;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.repository.*;
import com.lwe.rules.D20RuleEngine;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CombatServiceTest {

    private final CombatSessionRepository sessionRepo = mock();
    private final CombatParticipantRepository participantRepo = mock();
    private final GameEntityRepository entityRepo = mock();
    private final WorldRepository worldRepo = mock();
    private final GameSystemRepository gameSystemRepo = mock();
    private final WorldEventService eventService = mock();
    private final RollService rollService = mock();
    private final AbilityRepository abilityRepo = mock();
    private final SimpMessagingTemplate messaging = mock();
    private final WorldAccess worldAccess = mock();
    private final RulesLoader rulesLoader = mock();
    private final CampaignMemberService campaignMemberService = mock();
    private final ConditionService conditionService = mock();

    private CombatService combatService;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        combatService = new CombatService(sessionRepo, participantRepo, entityRepo,
            worldRepo, gameSystemRepo, eventService, rollService, abilityRepo, messaging, worldAccess, List.of(new D20RuleEngine()),
            new ObjectMapper(), rulesLoader, campaignMemberService, conditionService);
    }

    @Test
    void shouldStartCombat() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"geschicklichkeit\":14,\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"geschicklichkeit\":8,\"staerke\":12}");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(entityRepo.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<CombatSession>getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });
        when(participantRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var session = combatService.startCombat(userId, worldId, List.of(attackerId, defenderId));

        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo("ACTIVE");
        assertThat(session.getCurrentTurnEntityId()).isNotNull();
        verify(participantRepo).saveAll(any());
        verify(eventService).publish(eq(worldId), isNull(), any(WorldEventService.EventType.class), any(), any(), any());
    }

    @Test
    void shouldStartCombatWithMapId() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var mapId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"geschicklichkeit\":14,\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"geschicklichkeit\":8,\"staerke\":12}");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(entityRepo.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<CombatSession>getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });
        when(participantRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var session = combatService.startCombat(userId, worldId, List.of(attackerId, defenderId), mapId);

        assertThat(session).isNotNull();
        assertThat(session.getMapId()).isEqualTo(mapId);
    }

    @Test
    void dmCanStartCampaignCombat() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var campaignId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"geschicklichkeit\":14,\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"geschicklichkeit\":8,\"staerke\":12}");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), "{}");
        setId(world, worldId);

        when(entityRepo.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(campaignMemberService.isDm(campaignId, userId)).thenReturn(true);
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<CombatSession>getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });
        when(participantRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var session = combatService.startCombat(userId, worldId, List.of(attackerId, defenderId), null, campaignId);

        assertThat(session.getCampaignId()).isEqualTo(campaignId);
        verify(campaignMemberService).isDm(campaignId, userId);
    }

    @Test
    void nonDmNonOwnerCannotStartCampaignCombat() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var campaignId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), "{}");
        setId(world, worldId);

        when(entityRepo.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(campaignMemberService.isDm(campaignId, userId)).thenReturn(false);

        assertThatThrownBy(() -> combatService.startCombat(userId, worldId, List.of(attackerId, defenderId), null, campaignId))
            .isInstanceOf(CombatService.CombatException.class)
            .matches(e -> ((CombatService.CombatException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    @Test
    void shouldExecuteAttack() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"staerke\":12,\"position_json\":\"{\\\"x\\\":5,\\\"y\\\":5}\"}");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        session.setCurrentTurnEntityId(attackerId);

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));

        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(10);
        defenderParticipant.setHpMax(10);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(List.of(participant, defenderParticipant));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("attack", "1d8+3", new int[]{5}, 8, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, session.getId(), attackerId, "ATTACK", defenderId, null);

        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo("ATTACK");
        assertThat(result.success()).isTrue();
        verify(participantRepo, atLeast(1)).save(any());
    }

    @Test
    void actionTypeActionDealsDamage() {
        var campaignId = UUID.randomUUID();
        var session = new CombatSession(worldId, campaignId);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"staerke\":12}");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        session.setCurrentTurnEntityId(attackerId);

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));

        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(10);
        defenderParticipant.setHpMax(10);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(List.of(participant, defenderParticipant)));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("attack", "1d8+3", new int[]{5}, 8, 0, true, null));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("attack", "1d8+3", new int[]{5}, 8, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, session.getId(), attackerId, "ACTION", defenderId, null);

        assertThat(result.totalDamage()).isGreaterThan(0);
        assertThat(defenderParticipant.getHpCurrent()).isLessThan(10);
    }

    @Test
    void actionTypeMatchingIsCaseInsensitive() {
        // Wizard-Systeme speichern action_types kleingeschrieben (["action"]),
        // die UI sendet "ACTION" — das muss als schädigend gelten (TDD BUG-3).
        var campaignId = UUID.randomUUID();
        var session = new CombatSession(worldId, campaignId);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setAttributesJson("{\"staerke\":16}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"staerke\":12}");
        setId(defender, defenderId);

        var gs = new GameSystem("Lower", 1,
            "{\"dice_mechanics\":{\"combat\":{\"action_types\":[\"action\"],\"damage\":\"1d6\"}}}", "{}");
        when(rulesLoader.loadSystemByCampaign(campaignId)).thenReturn(gs);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        session.setCurrentTurnEntityId(attackerId);

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));

        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(10);
        defenderParticipant.setHpMax(10);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(List.of(participant, defenderParticipant)));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("attack", "1d6", new int[]{4}, 4, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, session.getId(), attackerId, "ACTION", defenderId, null);

        assertThat(result.totalDamage()).isGreaterThan(0);
        assertThat(defenderParticipant.getHpCurrent()).isLessThan(10);
    }

    @Test
    void endCombatWritesParticipantHpBackToEntities() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        attacker.setHpCurrent(10); attacker.setHpMax(10);
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setHpCurrent(10); defender.setHpMax(10);
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var damaged = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        damaged.setHpCurrent(3);
        var healthy = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(List.of(healthy, damaged));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        combatService.endCombat(userId, session.getId());

        assertThat(defender.getHpCurrent()).isEqualTo(3);
        verify(entityRepo, atLeast(1)).save(any());
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void nextTurnTicksConditionsOfNextActor() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        session.setCurrentTurnEntityId(a);

        var pa = new CombatParticipant(session.getId(), a, 20, 2, "A");
        var pb = new CombatParticipant(session.getId(), b, 10, 2, "A");

        var entityB = new GameEntity(worldId, "PC", "B");
        setId(entityB, b);
        entityB.setMetadataJson("{\"conditions\":[{\"name\":\"Wunde\",\"rounds\":2}]}");

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(sessionRepo.findById(session.getId())).thenReturn(java.util.Optional.of(session));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(java.util.List.of(pa, pb)));
        when(entityRepo.findById(b)).thenReturn(java.util.Optional.of(entityB));
        when(conditionService.active(entityB))
            .thenReturn(java.util.List.of(new ConditionService.ConditionInstance("Wunde", 2)));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        combatService.nextTurn(userId, session.getId());

        verify(conditionService).tick(entityB);
        verify(entityRepo).save(entityB);
    }

    @Test
    void executeManeuverAddsDamageAndCostsAp() {
        var campaignId = UUID.randomUUID();
        var session = new CombatSession(worldId, campaignId);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);
        session.setCurrentTurnEntityId(attackerId);

        when(sessionRepo.findById(session.getId())).thenReturn(java.util.Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(java.util.Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(java.util.Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of(
            "dice_mechanics", Map.of("combat", Map.of(
                "maneuvers", java.util.List.of(Map.of(
                    "name", "Wuchtschlag",
                    "apCost", 2,
                    "effects", java.util.List.of(Map.of("target", "damage", "op", "add", "value", 3))))))));

        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(10);
        defenderParticipant.setHpMax(10);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(java.util.List.of(participant, defenderParticipant)));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("dmg", "1d8", new int[]{5}, 5, 0, true, null));
        when(participantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeManeuver(userId, session.getId(), attackerId, defenderId, "Wuchtschlag");

        assertThat(result.totalDamage()).isEqualTo(8); // 5 + 3
        assertThat(result.apRemaining()).isZero();     // 2 AP - 2
        assertThat(defenderParticipant.getHpCurrent()).isEqualTo(2);
    }

    @Test
    void executeManeuverRejectsInsufficientAp() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        session.setCurrentTurnEntityId(attackerId);
        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(sessionRepo.findById(session.getId())).thenReturn(java.util.Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(java.util.List.of(
                new CombatParticipant(session.getId(), attackerId, 15, 1, "A"))));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of(
            "dice_mechanics", Map.of("combat", Map.of(
                "maneuvers", java.util.List.of(Map.of("name", "Wuchtschlag", "apCost", 2))))));

        assertThatThrownBy(() -> combatService.executeManeuver(userId, session.getId(), attackerId, null, "Wuchtschlag"))
            .isInstanceOf(CombatService.CombatException.class)
            .satisfies(e -> assertThat(((CombatService.CombatException) e).getErrorCode())
                .isEqualTo("COMBAT_AP_INSUFFICIENT"));
    }

    @Test
    void startCombatTicksTimedConditionsOfFirstActor() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        attacker.setMetadataJson("{\"conditions\":[{\"name\":\"Wunde\",\"rounds\":1}]}");
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(entityRepo.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(entityRepo.findById(any())).thenReturn(java.util.Optional.of(attacker));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<CombatSession>getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });
        when(participantRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);
        when(conditionService.active(attacker)).thenReturn(java.util.List.of(
            new ConditionService.ConditionInstance("Wunde", 1)));

        combatService.startCombat(userId, worldId, List.of(attackerId, defenderId));

        verify(conditionService).tick(attacker);
    }

    @Test
    void executeManeuverRejectsUnknownName() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        session.setCurrentTurnEntityId(attackerId);
        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);
        when(sessionRepo.findById(session.getId())).thenReturn(java.util.Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(java.util.List.of(
                new CombatParticipant(session.getId(), attackerId, 15, 2, "A"))));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of());

        assertThatThrownBy(() -> combatService.executeManeuver(userId, session.getId(), attackerId, null, "Nix"))
            .isInstanceOf(CombatService.CombatException.class)
            .satisfies(e -> assertThat(((CombatService.CombatException) e).getErrorCode())
                .isEqualTo("COMBAT_MANEUVER_UNKNOWN"));
    }
}
