package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final com.lwe.core.util.WorldAccess worldAccess = mock();

    private final RulesLoader rulesLoader = mock();

    private final RollService service = new RollService(entityRepo, worldRepo,
        gameSystemRepo, eventService, List.of(new D20RuleEngine()),
            rulesLoader, new ObjectMapper(), worldAccess);

    @Test
    void shouldExecuteD20Roll() {
        var userId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();

        var entity = new GameEntity(worldId, "PC", "Hero");
        entity.setAttributesJson("{\"staerke\":16}");
        setId(entity, entityId);

        var world = new com.lwe.core.domain.World("W", userId, "{}");
        setId(world, worldId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

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
    void shouldAllowWorldMemberEvenIfNotOwner() {
        var ownerId = UUID.randomUUID();
        var memberId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var entity = new GameEntity(worldId, "PC", "H");
        setId(entity, entityId);
        var world = new com.lwe.core.domain.World("W", ownerId, "{}");
        setId(world, worldId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(eventService.publish(any(), any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = service.executeRoll(memberId, worldId, entityId, "staerke", 0, 10);

        assertThat(result.error()).isNull();
        verify(worldAccess).requireAccess(worldId, memberId);
    }

    @Test
    void shouldPropagateAccessDenied() {
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var entity = new GameEntity(worldId, "PC", "H");
        setId(entity, entityId);
        var world = new com.lwe.core.domain.World("W", UUID.randomUUID(), "{}");
        setId(world, worldId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        var stranger = UUID.randomUUID();
        doThrow(new com.lwe.core.util.WorldAccess.WorldAccessException(
            "WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, stranger);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.executeRoll(stranger, worldId, entityId, "x", 0, 10))
            .isInstanceOf(com.lwe.core.util.WorldAccess.WorldAccessException.class)
            .hasMessageContaining("Access");
    }

    private void setId(Object obj, UUID id) {
        try {
            var f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}