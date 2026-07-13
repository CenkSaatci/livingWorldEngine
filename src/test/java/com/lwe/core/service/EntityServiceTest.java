package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private WorldRepository worldRepo;

    private EntityService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final World world = new World("Test", userId, null, "{}");

    @BeforeEach
    void setUp() {
        setId(world, worldId);
        service = new EntityService(entityRepo, worldRepo);
    }

    @Test
    void shouldCreateEntity() {
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(entityRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<GameEntity>getArgument(0);
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, UUID.randomUUID());
            return e;
        });

        var result = service.create(worldId, userId, "NPC", "Goblin", null, null, null, null, null);

        assertThat(result.getName()).isEqualTo("Goblin");
        assertThat(result.getEntityType()).isEqualTo("NPC");
        verify(entityRepo).save(any());
    }

    @Test
    void shouldRejectAccessToOtherWorld() {
        var otherWorld = new World("Other", UUID.randomUUID(), null, "{}");
        setId(otherWorld, UUID.randomUUID());
        when(worldRepo.findById(otherWorld.getId())).thenReturn(Optional.of(otherWorld));

        assertThatThrownBy(() -> service.create(otherWorld.getId(), userId, "NPC", "X", null, null, null, null, null))
            .isInstanceOf(EntityService.EntityException.class)
            .matches(e -> ((EntityService.EntityException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    @Test
    void shouldThrowOnMissingEntity() {
        when(entityRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), userId))
            .isInstanceOf(EntityService.EntityException.class)
            .matches(e -> ((EntityService.EntityException) e).getErrorCode().equals("ENTITY_NOT_FOUND"));
    }

    private void setId(World w, UUID id) {
        try {
            var f = World.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(w, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}