package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.rules.D20RuleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RollServiceTest {

    private final GameEntityRepository entityRepo = mock();
    private final WorldRepository worldRepo = mock();
    private final GameSystemRepository gameSystemRepo = mock();
    private final WorldEventService eventService = mock();

    private final RollService service = new RollService(entityRepo, worldRepo,
        gameSystemRepo, eventService, List.of(new D20RuleEngine()));

    @Test
    void shouldExecuteD20Roll() {
        var userId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();

        var entity = new GameEntity(worldId, "PC", "Hero");
        entity.setAttributesJson("{\"staerke\":16}");
        setId(entity, entityId);

        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
        setId(world, worldId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = service.executeRoll(userId, worldId, entityId, "staerke", 0, 15);

        assertThat(result).isNotNull();
        assertThat(result.skillId()).isEqualTo("staerke");
        assertThat(result.dice()).hasSize(1);
        assertThat(result.total()).isBetween(4, 23);
        assertThat(result.error()).isNull();
    }

    @Test
    void shouldReturnErrorOnMissingEntity() {
        when(entityRepo.findById(any())).thenReturn(Optional.empty());
        var result = service.executeRoll(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "x", 0, 10);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotNull();
    }

    @Test
    void shouldReturnErrorOnWrongUser() {
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var entity = new GameEntity(worldId, "PC", "H");
        setId(entity, entityId);
        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), null, "{}");
        setId(world, worldId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        var result = service.executeRoll(UUID.randomUUID(), worldId, entityId, "x", 0, 10);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Access");
    }

    private void setId(Object obj, UUID id) {
        try {
            var f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}