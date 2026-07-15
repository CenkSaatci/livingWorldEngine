package com.lwe.core.service;

import com.lwe.core.domain.Region;
import com.lwe.core.domain.World;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.util.WorldAccess;
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
class RegionServiceTest {

    @Mock private RegionRepository repo;
    @Mock private WorldAccess worldAccess;
    @Mock private EntityEventService eventService;

    private RegionService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RegionService(repo, worldAccess, eventService);
    }

    @Test
    void shouldCreateRegion() {
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(null);

        var region = service.create(worldId, userId, "Schattental", null, null, 3, "forest", null, null, null, null);

        assertThat(region.getName()).isEqualTo("Schattental");
        assertThat(region.getDangerLevel()).isEqualTo(3);
        assertThat(region.getClimate()).isEqualTo("forest");
        verify(repo).save(any());
    }

    @Test
    void shouldRejectAccess() {
        doThrow(new RegionService.RegionException("WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, userId);

        assertThatThrownBy(() -> service.create(worldId, userId, "X", null, null, 1, "plain", null, null, null, null))
            .isInstanceOf(RegionService.RegionException.class)
            .matches(e -> ((RegionService.RegionException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
