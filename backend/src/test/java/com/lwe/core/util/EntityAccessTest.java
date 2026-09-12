package com.lwe.core.util;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.GameEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EntityAccessTest {

    private final GameEntityRepository entityRepo = mock();
    private final WorldAccess worldAccess = mock();
    private final EntityAccess entityAccess = new EntityAccess(entityRepo, worldAccess);

    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID entityId = UUID.randomUUID();

    private GameEntity entity(UUID owner) {
        var e = new GameEntity(worldId, "PC", "Held");
        e.setOwnerUserId(owner);
        try {
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, entityId);
        } catch (Exception ex) { throw new RuntimeException(ex); }
        return e;
    }

    @BeforeEach
    void stub() {
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity(userId)));
    }

    @Test
    void legacyEntityWithoutOwnerIsMemberLevel() {
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity(null)));

        entityAccess.requireControl(entityId, userId);

        verify(worldAccess).requireAccess(worldId, userId);
        verify(worldAccess, never()).requireDm(any(), any());
    }

    @Test
    void ownerControlsOwnEntity() {
        entityAccess.requireControl(entityId, userId);

        verify(worldAccess).requireAccess(worldId, userId);
        verify(worldAccess, never()).requireDm(any(), any());
    }

    @Test
    void nonOwnerNeedsDmRights() {
        var other = UUID.randomUUID();

        entityAccess.requireControl(entityId, other);

        verify(worldAccess).requireDm(worldId, other);
        verify(worldAccess, never()).requireAccess(any(), any());
    }

    @Test
    void missingEntityIs404() {
        when(entityRepo.findById(entityId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entityAccess.requireControl(entityId, userId))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .matches(e -> ((WorldAccess.WorldAccessException) e).getErrorCode().equals("ENTITY_NOT_FOUND"));
    }

    @Test
    void worldMismatchDenied() {
        assertThatThrownBy(() -> entityAccess.requireControl(entityId, userId, UUID.randomUUID()))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .matches(e -> ((WorldAccess.WorldAccessException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }
}
