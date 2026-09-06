package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.repository.GameEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {
    @Mock
    private GameEntityRepository entityRepo;
    @Mock
    private WorldAccess worldAccess;
    private EntityService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EntityService(entityRepo, worldAccess, new ObjectMapper());
    }

    @Test
    void shouldCreateEntity() {
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(entityRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<GameEntity>getArgument(0);
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, UUID.randomUUID());
            return e;
        });

        var result = service.create(worldId, userId, "NPC", "Goblin",
            null, null, null, null, null,
            null, null, null, null);

        assertThat(result.getName()).isEqualTo("Goblin");
        assertThat(result.getEntityType()).isEqualTo("NPC");
        verify(entityRepo).save(any());
    }

    @Test
    void shouldThrowOnMissingEntity() {
        when(entityRepo.findById(any())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), userId))
            .isInstanceOf(EntityService.EntityException.class)
            .matches(e -> ((EntityService.EntityException) e).getErrorCode().equals("ENTITY_NOT_FOUND"));
    }
}
