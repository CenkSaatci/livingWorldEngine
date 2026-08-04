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

    private CombatService combatService;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        combatService = new CombatService(sessionRepo, participantRepo, entityRepo,
            worldRepo, gameSystemRepo, eventService, rollService, abilityRepo, messaging, worldAccess, List.of(new D20RuleEngine()),
            new ObjectMapper(), rulesLoader, campaignMemberService);
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

        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
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

        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
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

        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), null, "{}");
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

        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), null, "{}");
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

        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
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

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}