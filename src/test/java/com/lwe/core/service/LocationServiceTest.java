package com.lwe.core.service;

import com.lwe.core.domain.Location;
import com.lwe.core.domain.Region;
import com.lwe.core.domain.World;
import com.lwe.core.repository.LocationRepository;
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
class LocationServiceTest {

    @Mock private LocationRepository repo;
    @Mock private RegionRepository regionRepo;
    @Mock private WorldAccess worldAccess;
    @Mock private EntityEventService eventService;

    private LocationService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LocationService(repo, regionRepo, worldAccess, eventService);
    }

    @Test
    void shouldCreateLocation() {
        var region = new Region(worldId, "R");
        setId(region, regionId);

        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(regionRepo.findById(regionId)).thenReturn(Optional.of(region));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(null);

        var loc = service.create(regionId, userId, "village", "Düsterburg", null, null, 200, 5,
            null, null, false, null);

        assertThat(loc.getName()).isEqualTo("Düsterburg");
        assertThat(loc.getType()).isEqualTo("village");
        assertThat(loc.getWealth()).isEqualTo(5);
        verify(repo).save(any());
    }

    @Test
    void shouldRejectAccess() {
        var region = new Region(worldId, "R");
        setId(region, regionId);

        doThrow(new LocationService.LocationException("WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, userId);
        when(regionRepo.findById(regionId)).thenReturn(Optional.of(region));

        assertThatThrownBy(() -> service.create(regionId, userId, "village", "X", null, null, 0, 5, null, null, false, null))
            .isInstanceOf(LocationService.LocationException.class)
            .matches(e -> ((LocationService.LocationException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
