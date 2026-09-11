package com.lwe.core.service;

import com.lwe.core.domain.World;
import com.lwe.core.domain.WorldMap;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
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
class WorldMapServiceTest {

    @Mock private WorldMapRepository mapRepo;
    @Mock private WorldRepository worldRepo;

    private WorldMapService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorldMapService(mapRepo, worldRepo);
    }

    @Test
    void shouldGetOrCreateExistingMap() {
        var map = new WorldMap(worldId);
        map.setName("existing");
        var world = new World("Test", userId, null, "{}");
        setId(world, worldId);

        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(mapRepo.findByWorldId(worldId)).thenReturn(Optional.of(map));

        var result = service.getOrCreate(worldId, userId);
        assertThat(result.getName()).isEqualTo("existing");
        verify(mapRepo, never()).save(any());
    }

    @Test
    void shouldCreateMapIfNotExists() {
        var world = new World("Test", userId, null, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(mapRepo.findByWorldId(worldId)).thenReturn(Optional.empty());
        when(mapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.getOrCreate(worldId, userId);
        assertThat(result).isNotNull();
        verify(mapRepo).save(any());
    }

    @Test
    void shouldUpdateMap() {
        var map = new WorldMap(worldId);
        var world = new World("Test", userId, null, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(mapRepo.findByWorldId(worldId)).thenReturn(Optional.of(map));
        when(mapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(worldId, userId, "/uploads/test.png", 800, 600);
        assertThat(result.getImageUrl()).isEqualTo("/uploads/test.png");
        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);
    }

    @Test
    void shouldRejectAccessToNonOwner() {
        var world = new World("Test", UUID.randomUUID(), null, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        assertThatThrownBy(() -> service.getOrCreate(worldId, userId))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void shouldRejectUnknownWorld() {
        when(worldRepo.findById(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreate(worldId, userId))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .hasMessageContaining("World not found");
    }

    @Test
    void shouldRejectUnknownMap() {
        when(mapRepo.findById(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(worldId, userId))
            .isInstanceOf(WorldAccess.WorldAccessException.class)
            .hasMessageContaining("Map not found");
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
