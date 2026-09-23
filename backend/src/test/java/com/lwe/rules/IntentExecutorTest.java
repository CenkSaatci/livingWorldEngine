package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.NpcIntent;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.service.FactionService;
import com.lwe.core.service.RulesLoader;
import com.lwe.core.service.WorldEventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * A4: NPC-Angriffe müssen den Schaden tatsächlich auf die Ziel-HP anwenden.
 */
class IntentExecutorTest {

    private final GameEntityRepository entityRepo = mock();
    private final WorldEventService eventService = mock();
    private final FactionService factionService = mock();
    private final RulesLoader rulesLoader = mock();

    private final IntentExecutor executor = new IntentExecutor(entityRepo, eventService,
        factionService, new ObjectMapper(), rulesLoader);

    private final UUID worldId = UUID.randomUUID();
    private final UUID npcId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    private GameEntity entity(UUID id, UUID world, int hp) {
        var e = new GameEntity(world, "NPC", "E-" + id.toString().substring(0, 4));
        e.setHpMax(Math.max(hp, 10));
        e.setHpCurrent(hp);
        setId(e, id);
        return e;
    }

    private void stubDamage() {
        when(rulesLoader.loadRules(any(), eq(worldId))).thenReturn(Map.of(
            "dice_mechanics", Map.of("combat", Map.of("damage", "1d6+10"))));
    }

    @Test
    void attackAppliesDamageToTargetHp() {
        var npc = entity(npcId, worldId, 10);
        var target = entity(targetId, worldId, 5);
        when(entityRepo.findById(npcId)).thenReturn(Optional.of(npc));
        when(entityRepo.findById(targetId)).thenReturn(Optional.of(target));
        stubDamage();

        executor.execute(new NpcIntent(worldId, npcId, "ATTACK",
            "{\"target_id\":\"" + targetId + "\"}", "greift an"));

        // 1d6+10 >= 11, Ziel hat 5 HP → auf 0, angewandt 5.
        assertThat(target.getHpCurrent()).isZero();
        verify(entityRepo).save(target);
        var payload = ArgumentCaptor.forClass(Map.class);
        verify(eventService).publish(eq(worldId), eq(WorldEventService.EventType.COMBAT_ACTION_EXECUTED),
            eq(npcId), eq(targetId), payload.capture());
        assertThat(payload.getValue()).containsEntry("damage", 5);
    }

    @Test
    void attackWithoutTargetOnlyEmitsEvent() {
        var npc = entity(npcId, worldId, 10);
        when(entityRepo.findById(npcId)).thenReturn(Optional.of(npc));
        stubDamage();

        executor.execute(new NpcIntent(worldId, npcId, "ATTACK", "{}", "ins Leere"));

        verify(entityRepo, never()).save(any());
        verify(eventService).publish(eq(worldId), eq(WorldEventService.EventType.COMBAT_ACTION_EXECUTED),
            eq(npcId), isNull(), any());
    }

    @Test
    void attackIgnoresTargetFromOtherWorld() {
        var npc = entity(npcId, worldId, 10);
        var foreign = entity(targetId, UUID.randomUUID(), 5);
        when(entityRepo.findById(npcId)).thenReturn(Optional.of(npc));
        when(entityRepo.findById(targetId)).thenReturn(Optional.of(foreign));
        stubDamage();

        executor.execute(new NpcIntent(worldId, npcId, "ATTACK",
            "{\"target_id\":\"" + targetId + "\"}", "fremd"));

        assertThat(foreign.getHpCurrent()).isEqualTo(5);
        verify(entityRepo, never()).save(any());
    }

    private static void setId(Object obj, UUID id) {
        try {
            var f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
