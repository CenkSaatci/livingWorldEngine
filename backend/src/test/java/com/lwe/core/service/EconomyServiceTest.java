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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EconomyServiceTest {

    private final LocationRepository locationRepo = mock();
    private final GameEntityRepository entityRepo = mock();
    private final com.lwe.core.repository.RegionRepository regionRepo = mock();
    private final com.lwe.core.util.WorldAccess worldAccess = mock();

    private EconomyService economyService;
    private final UUID userId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        economyService = new EconomyService(locationRepo, entityRepo, regionRepo, worldAccess, new ObjectMapper());
    }

    private void stubWorld() {
        var regionId = UUID.randomUUID();
        var location = new Location(regionId, "village", "Bree");
        when(locationRepo.findById(locationId)).thenReturn(Optional.of(location));
        var region = new com.lwe.core.domain.Region(worldId, "Mittelreich");
        when(regionRepo.findById(regionId)).thenReturn(Optional.of(region));
    }

    @Test
    void shouldComputePricesBasedOnWealth() {
        stubWorld();
        var location = locationRepo.findById(locationId).orElseThrow();
        location.setWealth(7);

        var npc = new GameEntity(UUID.randomUUID(), "NPC", "Hugh");
        npc.setMetadataJson("""
            {"services_offered":["sell_weapons","repair"],"price_modifier":1.0}
            """);
        when(entityRepo.findByWorldIdAndMetadataJsonFilter(eq(worldId), anyString())).thenReturn(List.of(npc));

        var market = economyService.getMarketPrices(locationId, userId);

        assertThat(market).hasSize(2);
        var weaponEntry = market.stream().filter(e -> "sell_weapons".equals(e.get("service"))).findFirst().orElseThrow();
        // wealth=7 → wealthFactor = 1 + (7-5)*0.1 = 1.2, base=25, final = round(25 * 1.2 * 1.0) = 30
        assertThat(weaponEntry.get("final_price")).isEqualTo(30);
    }

    @Test
    void shouldApplyNpcPriceModifier() {
        stubWorld();
        var location = locationRepo.findById(locationId).orElseThrow();
        location.setWealth(5);

        var npc = new GameEntity(UUID.randomUUID(), "NPC", "Greedy Gus");
        npc.setMetadataJson("""
            {"services_offered":["sell_weapons"],"price_modifier":1.5}
            """);
        when(entityRepo.findByWorldIdAndMetadataJsonFilter(eq(worldId), anyString())).thenReturn(List.of(npc));

        var market = economyService.getMarketPrices(locationId, userId);

        assertThat(market).hasSize(1);
        var entry = market.getFirst();
        // wealth=5 → wealthFactor = 1.0, base=25, final = round(25 * 1.0 * 1.5) = 38
        assertThat(entry.get("final_price")).isEqualTo(38);
    }

    @Test
    void foreignWorldNpcsExcluded() {
        stubWorld();
        var foreign = new GameEntity(UUID.randomUUID(), "NPC", "Fremd");
        foreign.setMetadataJson("{\"services_offered\":[\"healing\"]}");
        // Welt-scoped Query liefert nur eigene NPCs
        when(entityRepo.findByWorldIdAndMetadataJsonFilter(eq(worldId), anyString()))
            .thenReturn(List.of());

        var market = economyService.getMarketPrices(locationId, userId);

        assertThat(market).isEmpty();
        verify(entityRepo, never()).findByMetadataJsonFilter(anyString());
    }

    @Test
    void strangerDeniedMarket() {
        stubWorld();
        doThrow(new com.lwe.core.util.WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "denied"))
            .when(worldAccess).requireRead(worldId, userId);

        assertThatThrownBy(() -> economyService.getMarketPrices(locationId, userId))
            .isInstanceOf(com.lwe.core.util.WorldAccess.WorldAccessException.class);
    }

    @Test
    void shouldHandleLocationNotFound() {
        when(locationRepo.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> economyService.getMarketPrices(locationId, userId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("LOCATION_NOT_FOUND");
    }
}
