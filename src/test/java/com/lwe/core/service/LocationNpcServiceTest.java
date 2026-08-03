package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LocationNpcServiceTest {

    private final GameEntityRepository entityRepo = mock();
    private final LocationRepository locationRepo = mock();

    private LocationNpcService locationNpcService;
    private final UUID userId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        locationNpcService = new LocationNpcService(entityRepo, locationRepo, new ObjectMapper());
        when(locationRepo.findById(locationId)).thenReturn(Optional.of(new Location(UUID.randomUUID(), "village", "Bree")));
    }

    @Test
    void shouldReturnNpcsAtLocation() {
        var npc = new GameEntity(UUID.randomUUID(), "NPC", "Elara");
        var merchant = new GameEntity(UUID.randomUUID(), "NPC", "Balin");

        when(entityRepo.findByMetadataJsonFilter(anyString())).thenReturn(List.of(npc, merchant));

        var result = locationNpcService.getNpcsAtLocation(locationId, userId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GameEntity::getName).containsExactlyInAnyOrder("Elara", "Balin");
    }

    @Test
    void shouldFilterOutNonNpcs() {
        var npc = new GameEntity(UUID.randomUUID(), "NPC", "Elara");
        var pc = new GameEntity(UUID.randomUUID(), "PC", "Hero");

        when(entityRepo.findByMetadataJsonFilter(anyString())).thenReturn(List.of(npc, pc));

        var result = locationNpcService.getNpcsAtLocation(locationId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Elara");
    }

    @Test
    void shouldReturnParsedServices() {
        var npc = new GameEntity(UUID.randomUUID(), "NPC", "Elara");
        npc.setMetadataJson("{\"occupation\":\"alchemist\",\"services_offered\":[\"sell_potions\",\"identification\"],\"greeting\":\"Welcome!\"}");
        var blacksmith = new GameEntity(UUID.randomUUID(), "NPC", "Balin");
        blacksmith.setMetadataJson("{\"occupation\":\"blacksmith\",\"services_offered\":[\"repair\",\"sell_weapons\"],\"greeting\":\"Need a blade?\"}");

        when(entityRepo.findByMetadataJsonFilter(anyString())).thenReturn(List.of(npc, blacksmith));

        var services = locationNpcService.getServices(locationId, userId);

        assertThat(services).isNotEmpty();
        assertThat(services.keySet()).contains("sell_potions", "repair");
    }
}
