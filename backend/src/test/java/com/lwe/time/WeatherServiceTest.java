package com.lwe.time;

import com.lwe.core.domain.Region;
import com.lwe.core.domain.RegionWeather;
import com.lwe.core.domain.World;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.RegionWeatherRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.EntityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock private WorldRepository worldRepo;
    @Mock private RegionRepository regionRepo;
    @Mock private RegionWeatherRepository weatherRepo;
    @Mock private EntityEventService entityEventService;
    @Mock private RandomGenerator mockRng;

    private WeatherService service;

    @BeforeEach
    void setUp() {
        service = new WeatherService(worldRepo, regionRepo, weatherRepo, entityEventService);
        ReflectionTestUtils.setField(service, "rng", mockRng);
    }

    @Test
    void rollWeatherShouldGenerateBasedOnClimate() {
        var region = new Region(UUID.randomUUID(), "desert-region");
        region.setClimate("desert");

        when(mockRng.nextInt(0, 7)).thenReturn(3);
        when(mockRng.nextInt(anyInt())).thenReturn(0);
        when(weatherRepo.findById(any())).thenReturn(Optional.empty());
        when(weatherRepo.save(any())).thenAnswer(inv -> inv.<RegionWeather>getArgument(0));

        var rw = service.rollWeather(region);

        assertThat(rw.getWeatherType()).isEqualTo("CLEAR");
        assertThat(rw.getTemperature()).isBetween(-20, 50);
        assertThat(rw.getWind()).isBetween(0, 10);
        verify(entityEventService).publish(eq("region"), eq(region.getId()), any(), any(), any(), anyInt(), any());
    }

    @Test
    void rollWeatherShouldAdjustTemperatureByWeather() {
        var region = new Region(UUID.randomUUID(), "arctic-region");
        region.setClimate("arctic");

        when(mockRng.nextInt(0, 7)).thenReturn(1);
        when(mockRng.nextInt(anyInt())).thenReturn(0);
        when(weatherRepo.findById(any())).thenReturn(Optional.empty());
        when(weatherRepo.save(any())).thenAnswer(inv -> inv.<RegionWeather>getArgument(0));

        var rw = service.rollWeather(region);

        assertThat(rw.getWeatherType()).isEqualTo("SNOW");
        assertThat(rw.getTemperature()).isBetween(-20, 50);
    }

    @Test
    void getWeatherShouldReturnExisting() {
        var regionId = UUID.randomUUID();
        var rw = new RegionWeather(regionId);
        when(weatherRepo.findById(regionId)).thenReturn(Optional.of(rw));

        assertThat(service.getWeather(regionId)).isSameAs(rw);
    }

    @Test
    void getWeatherShouldThrowWhenNotFound() {
        var regionId = UUID.randomUUID();
        when(weatherRepo.findById(regionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWeather(regionId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("WEATHER_NOT_FOUND");
    }

    @Test
    void tickAllWeatherShouldRollForActiveWorlds() {
        var world = new World("test", UUID.randomUUID(), null, "{}");
        world.setActive(true);
        var region = new Region(world.getId(), "test-region");
        region.setClimate("forest");

        when(worldRepo.findByActiveTrue()).thenReturn(List.of(world));
        when(regionRepo.findByWorldIdOrderByNameAsc(world.getId())).thenReturn(List.of(region));
        when(mockRng.nextDouble()).thenReturn(0.0);
        when(mockRng.nextInt(0, 7)).thenReturn(2);
        when(mockRng.nextInt(anyInt())).thenReturn(0);
        when(weatherRepo.findById(any())).thenReturn(Optional.empty());
        when(weatherRepo.save(any())).thenAnswer(inv -> inv.<RegionWeather>getArgument(0));

        service.tickAllWeather();

        verify(weatherRepo).save(any());
        verify(entityEventService).publish(any(), any(), any(), any(), any(), anyInt(), any());
    }
}
