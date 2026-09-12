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
    private final com.lwe.core.repository.GameItemRepository itemRepo = mock();
    private final DerivedValueService derivedValueService = mock();
    private final EntityService entityService = mock();

    private CombatService combatService;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        combatService = new CombatService(sessionRepo, participantRepo, entityRepo,
            worldRepo, gameSystemRepo, eventService, rollService, abilityRepo, messaging, worldAccess,
            new com.lwe.core.util.EntityAccess(entityRepo, worldAccess), List.of(new D20RuleEngine()),
            new ObjectMapper(), rulesLoader, campaignMemberService, conditionService, itemRepo,
            derivedValueService, entityService);
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
    void startCombatUsesEntityHp() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"geschicklichkeit\":14}");
        attacker.setHpMax(16);
        attacker.setHpCurrent(12);
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"geschicklichkeit\":8}");
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

        combatService.startCombat(userId, worldId, List.of(attackerId, defenderId));

        var captor = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        verify(participantRepo).saveAll(captor.capture());
        var saved = (List<CombatParticipant>) captor.getValue();
        var held = saved.stream().filter(p -> p.getEntityId().equals(attackerId)).findFirst().orElseThrow();
        assertThat(held.getHpMax()).isEqualTo(16);
        assertThat(held.getHpCurrent()).isEqualTo(12);
    }

    @Test
    void findActiveSessionReturnsLatestActive() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        when(sessionRepo.findFirstByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "ACTIVE"))
            .thenReturn(Optional.of(session));

        var result = combatService.findActiveSession(userId, worldId);

        assertThat(result).isPresent();
        verify(worldAccess).requireAccess(worldId, userId);
    }

    @Test
    void findActiveSessionEmptyWhenNone() {
        when(sessionRepo.findFirstByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "ACTIVE"))
            .thenReturn(Optional.empty());

        assertThat(combatService.findActiveSession(userId, worldId)).isEmpty();
    }

    @Test
    void attackOnDefeatedTargetIsRejected() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Held");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var participant = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var downTarget = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        downTarget.setHpCurrent(0);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(participant, downTarget)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));

        assertThatThrownBy(() -> combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null))
            .isInstanceOf(CombatService.CombatException.class)
            .matches(e -> ((CombatService.CombatException) e).getErrorCode().equals("COMBAT_TARGET_DEFEATED"));
    }

    @Test
    void defeatedActorCannotAct() {
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Held");
        setId(attacker, attackerId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var downActor = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        downActor.setHpCurrent(0);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);
        var defenderParticipant = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(downActor, defenderParticipant)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));

        assertThatThrownBy(() -> combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null))
            .isInstanceOf(CombatService.CombatException.class)
            .matches(e -> ((CombatService.CombatException) e).getErrorCode().equals("COMBAT_ACTOR_DEFEATED"));
    }

    private Map<String, Object> attackRules() {
        return Map.of("dice_mechanics", Map.of("combat", Map.of(
            "initiative", "1d20", "damage", "1d8",
            "attack", Map.of("attribute", "geschick", "target", "ac", "dice", "1d20"))));
    }

    private Map<String, Object> attackRulesLte() {
        return Map.of("dice_mechanics", Map.of("combat", Map.of(
            "initiative", "1d20", "damage", "1d8",
            "attack", Map.of("attribute", "geschick", "target", "ac", "dice", "1d20", "comparison", "lte"))));
    }

    private void stubDerivedAc(double ac) {
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(
            List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("ac", ac, null)));
    }

    @Test
    void attackGateMissesWhenTargetNotReached() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"geschick\":10}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(attackRules());
        stubDerivedAc(99); // unerreichbar fuer 1d20
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null);

        assertThat(result.actionType()).isEqualTo("MISS");
        assertThat(result.totalDamage()).isZero();
        assertThat(pd.getHpCurrent()).isEqualTo(pd.getHpMax());
        assertThat(pa.getApCurrent()).isEqualTo(1); // Audit P1: AP wird trotzdem verbraucht
    }

    @Test
    void attackGateHitsAndDealsDamage() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"geschick\":10}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(attackRules());
        stubDerivedAc(1); // immer treffer
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{5}, 8, 0, true, null));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{5}, 8, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null);

        assertThat(result.totalDamage()).isGreaterThan(0);
        assertThat(pd.getHpCurrent()).isLessThan(pd.getHpMax());
    }

    @Test
    void attackGateRespectsComparisonLte() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"geschick\":10}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Kultist");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(attackRulesLte());
        stubDerivedAc(0); // lte: total <= 0 ist mit 1d20 nie erfuellt -> MISS deterministisch
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null);

        assertThat(result.actionType()).isEqualTo("MISS");
    }

    @Test
    void fatePointAvoidsDeathAndLeavesTargetAtOneHp() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"geschick\":10}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setMetadataJson("{\"fate_points\":1}");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        pd.setHpCurrent(5);
        pd.setHpMax(5);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of("dice_mechanics", Map.of("combat", Map.of(
            "initiative", "1d20", "damage", "1d8",
            "attack", Map.of("attribute", "geschick", "target", "ac", "dice", "1d20"))),
            "fate", Map.of("avoidDeathCost", 1)));
        stubDerivedAc(1); // immer Treffer
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{8}, 8, 0, true, null));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{8}, 8, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);
        when(entityService.spendFatePointsIfAvailable(eq(defenderId), eq(userId), any(), eq(1)))
            .thenReturn(true);

        combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null);

        assertThat(pd.getHpCurrent()).isEqualTo(1); // statt 0 (Tod abgewendet)
        verify(entityService).spendFatePointsIfAvailable(eq(defenderId), eq(userId), any(), eq(1));
    }

    @Test
    void blockedConditionPreventsAttackAction() {        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setMetadataJson("{\"conditions\":[\"Betaeubt\"]}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of(
            "conditions", List.of(Map.of("name", "Betaeubt", "blocks", List.of("ATTACK")))));
        when(conditionService.blockedActions(any(), any())).thenReturn(List.of("ATTACK"));

        assertThatThrownBy(() -> combatService.executeAction(
            userId, sessionId, attackerId, "ATTACK", defenderId, null))
            .isInstanceOf(CombatService.CombatException.class)
            .satisfies(e -> assertThat(((CombatService.CombatException) e).getErrorCode())
                .isEqualTo("COMBAT_ACTION_BLOCKED"));
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

        var entityA = new GameEntity(worldId, "PC", "A");
        setId(entityA, a);

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
        when(entityRepo.findById(a)).thenReturn(java.util.Optional.of(entityA));
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
    void attackWithValueSourceRollsUnderOwnDerivedValue() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"at\":30}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"pa\":10}");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of("dice_mechanics", Map.of("combat", Map.of(
            "initiative", "1d20", "damage", "1d8",
            "attack", Map.of("value", "at", "target", "pa", "dice", "1d20", "comparison", "lte")))));
        when(derivedValueService.evaluate(any(), any(), any())).thenAnswer(inv -> {
            java.util.Map<String, Integer> attrs = inv.getArgument(1);
            return attrs.containsKey("at")
                ? List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("at", 30, null))
                : List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("pa", 10, null));
        });
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{5}, 5, 0, true, null));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("damage", "1d8", new int[]{5}, 5, 0, true, null));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, sessionId, attackerId, "ACTION", defenderId, null);

        // 1d20 <= 30 ist immer erfuellt -> deterministischer Treffer
        assertThat(result.totalDamage()).isGreaterThan(0);
    }

    @Test
    void maneuverWithAttackMalusCanMiss() {
        var sessionId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var attacker = new GameEntity(worldId, "PC", "Held");
        attacker.setAttributesJson("{\"at\":20}");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Ork");
        defender.setAttributesJson("{\"pa\":10}");
        setId(defender, defenderId);

        var session = new CombatSession(worldId, null);
        setId(session, sessionId);
        session.setCurrentTurnEntityId(attackerId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        var pa = new CombatParticipant(sessionId, attackerId, 10, 2, "A");
        var pd = new CombatParticipant(sessionId, defenderId, 5, 2, "B");
        pd.setHpCurrent(10);
        pd.setHpMax(10);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(sessionId))
            .thenReturn(new java.util.ArrayList<>(List.of(pa, pd)));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of("dice_mechanics", Map.of("combat", Map.of(
            "initiative", "1d20", "damage", "1d8",
            "attack", Map.of("value", "at", "target", "pa", "dice", "1d20", "comparison", "lte"),
            "maneuvers", List.of(Map.of(
                "name", "Wuchtschlag", "apCost", 2, "attackMalus", 21,
                "effects", List.of(Map.of("target", "damage", "op", "add", "value", 3))))))));
        when(derivedValueService.evaluate(any(), any(), any())).thenAnswer(inv -> {
            java.util.Map<String, Integer> attrs = inv.getArgument(1);
            return attrs.containsKey("at")
                ? List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("at", 20, null))
                : List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("pa", 10, null));
        });
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeManeuver(userId, sessionId, attackerId, defenderId, "Wuchtschlag");

        // Wurf + 21 > 20 ist immer erfuellt -> deterministischer Miss, AP trotzdem weg.
        assertThat(result.actionType()).isEqualTo("MISS");
        assertThat(result.totalDamage()).isZero();
        assertThat(pd.getHpCurrent()).isEqualTo(10);
        assertThat(pa.getApCurrent()).isZero();
    }


    @Test
    void weaponDamageTypeResistanceHalvesDamage() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var itemId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Feuerelementar");
        defender.setMetadataJson("{\"damage_resistances\":[\"fire\"]}");
        setId(defender, defenderId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);
        session.setCurrentTurnEntityId(attackerId);

        attacker.setInventoryJson("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":true}]");
        var weapon = mock(GameItem.class);
        when(weapon.getMetadataJson()).thenReturn("{\"damage_type\":\"fire\"}");

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(weapon));
        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(10);
        defenderParticipant.setHpMax(10);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(List.of(participant, defenderParticipant)));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(new RollService.RollResult("attack", "1d8+3", new int[]{5}, 8, 0, true, null));
        when(participantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, session.getId(), attackerId, "ATTACK", defenderId, itemId);

        assertThat(result.totalDamage()).isEqualTo(4); // 8 / 2
        assertThat(defenderParticipant.getHpCurrent()).isEqualTo(6);
    }

    @Test
    void abilityDamageTypeVulnerabilityDoublesDamage() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();
        var abilityId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Magier");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Eiselementar");
        defender.setMetadataJson("{\"damage_vulnerabilities\":[\"cold\"]}");
        setId(defender, defenderId);
        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);
        session.setCurrentTurnEntityId(attackerId);

        var ability = new Ability(worldId, "Froststrahl", Ability.AbilityType.ACTIVE);
        ability.setEffectsJson("{\"damage\":\"1d6\",\"damageType\":\"cold\"}");

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(entityRepo.findById(defenderId)).thenReturn(Optional.of(defender));
        when(abilityRepo.findById(abilityId)).thenReturn(Optional.of(ability));
        var participant = new CombatParticipant(session.getId(), attackerId, 15, 2, "A");
        var defenderParticipant = new CombatParticipant(session.getId(), defenderId, 10, 2, "A");
        defenderParticipant.setHpCurrent(12);
        defenderParticipant.setHpMax(12);
        when(participantRepo.findByCombatIdOrderByInitiativeDesc(session.getId()))
            .thenReturn(new java.util.ArrayList<>(List.of(participant, defenderParticipant)));
        when(rollService.executeRoll(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new RollService.RollResult("dmg", "1d6", new int[]{5}, 5, 0, true, null));
        when(participantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        combatService.useAbility(userId, session.getId(), attackerId, abilityId, defenderId);

        assertThat(defenderParticipant.getHpCurrent()).isEqualTo(2); // 12 - 10
    }

    @Test
    void armorReducesDamage() {
        var session = new CombatSession(worldId, null);
        setId(session, UUID.randomUUID());
        var attackerId = UUID.randomUUID();
        var defenderId = UUID.randomUUID();

        var attacker = new GameEntity(worldId, "PC", "Aragorn");
        setId(attacker, attackerId);
        var defender = new GameEntity(worldId, "NPC", "Gepanzerter");
        defender.setMetadataJson("{\"damage_armor\":3}");
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
        when(participantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = combatService.executeAction(userId, session.getId(), attackerId, "ATTACK", defenderId, null);

        assertThat(result.totalDamage()).isEqualTo(5); // 8 - 3 Ruestung
        assertThat(defenderParticipant.getHpCurrent()).isEqualTo(5);
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
        when(entityRepo.findById(attackerId)).thenReturn(java.util.Optional.of(attacker));
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
        when(entityRepo.findById(attackerId)).thenReturn(java.util.Optional.of(attacker));
        when(rulesLoader.loadRules(any(), any())).thenReturn(Map.of());

        assertThatThrownBy(() -> combatService.executeManeuver(userId, session.getId(), attackerId, null, "Nix"))
            .isInstanceOf(CombatService.CombatException.class)
            .satisfies(e -> assertThat(((CombatService.CombatException) e).getErrorCode())
                .isEqualTo("COMBAT_MANEUVER_UNKNOWN"));
    }
}
