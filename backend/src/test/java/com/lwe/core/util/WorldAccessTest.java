package com.lwe.core.util;

import com.lwe.core.domain.World;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldAccessTest {

    @Mock private WorldMemberRepository memberRepo;
    @Mock private WorldRepository worldRepo;

    private World world(UUID ownerId) {
        return new World("Test", ownerId, "{}");
    }

    @Test
    void ownerHasAccess() {
        var ownerId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world(ownerId)));

        assertThatNoException().isThrownBy(
            () -> new WorldAccess(memberRepo, worldRepo).requireAccess(worldId, ownerId));
    }

    @Test
    void memberHasAccess() {
        var ownerId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world(ownerId)));
        when(memberRepo.existsByWorldIdAndUserId(worldId, userId)).thenReturn(true);

        assertThatNoException().isThrownBy(
            () -> new WorldAccess(memberRepo, worldRepo).requireAccess(worldId, userId));
    }

    @Test
    void strangerIsDenied() {
        var worldId = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world(UUID.randomUUID())));
        when(memberRepo.existsByWorldIdAndUserId(worldId, stranger)).thenReturn(false);

        assertThatThrownBy(
            () -> new WorldAccess(memberRepo, worldRepo).requireAccess(worldId, stranger))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void deletedWorldIsDenied() {
        var ownerId = UUID.randomUUID();
        var worldId = UUID.randomUUID();
        var world = world(ownerId);
        world.setActive(false);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new WorldAccess(memberRepo, worldRepo).requireAccess(worldId, ownerId))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .matches(e -> ((WorldAccess.WorldAccessException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    @Test
    void missingWorldThrowsNotFound() {
        var worldId = UUID.randomUUID();
        when(worldRepo.findById(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> new WorldAccess(memberRepo, worldRepo).requireAccess(worldId, UUID.randomUUID()))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
    }
}
