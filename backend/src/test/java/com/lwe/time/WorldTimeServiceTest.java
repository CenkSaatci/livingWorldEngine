package com.lwe.time;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.World;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.WorldEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldTimeServiceTest {

    @Mock private WorldRepository worldRepo;
    @Mock private WorldEventService eventService;

    private WorldTimeService service;

    @BeforeEach
    void setUp() {
        service = new WorldTimeService(worldRepo, eventService, new ObjectMapper());
    }

    @Test
    void tickAllWorldsShouldAdvanceAutomaticWorlds() {
        var world = new World("test", UUID.randomUUID(), "{\"time\":{\"mode\":\"automatic\",\"tick_interval_real_seconds\":1,\"tick_advance_game_minutes\":60,\"paused\":false}}");
        world.setActive(true);
        world.setCurrentGameTime(Instant.now().minus(Duration.ofHours(2)));
        world.setLastTickAt(Instant.now().minusSeconds(10));

        when(worldRepo.findByActiveTrue()).thenReturn(List.of(world));
        when(worldRepo.save(any())).thenAnswer(inv -> inv.<World>getArgument(0));

        service.tickAllWorlds();

        assertThat(world.getLastTickAt()).isNotNull();
        verify(worldRepo, atLeast(2)).save(any());
    }

    @Test
    void tickAllWorldsShouldSkipManualWorlds() {
        var world = new World("test", UUID.randomUUID(), "{\"time\":{\"mode\":\"manual\",\"tick_interval_real_seconds\":1,\"tick_advance_game_minutes\":60,\"paused\":false}}");
        world.setActive(true);

        when(worldRepo.findByActiveTrue()).thenReturn(List.of(world));

        service.tickAllWorlds();

        verify(worldRepo, never()).save(any());
    }

    @Test
    void dayPhaseShouldReturnCorrectPhase() {
        var world = new World("test", UUID.randomUUID(), "{\"time\":{\"mode\":\"automatic\",\"tick_interval_real_seconds\":60,\"tick_advance_game_minutes\":60,\"paused\":false,\"day_starts_at_hour\":6}}");

        world.setCurrentGameTime(atHour(6));
        assertThat(service.dayPhase(world)).isEqualTo(WorldTimeService.DayPhase.DAY);

        world.setCurrentGameTime(atHour(5));
        assertThat(service.dayPhase(world)).isEqualTo(WorldTimeService.DayPhase.DAWN);

        world.setCurrentGameTime(atHour(17));
        assertThat(service.dayPhase(world)).isEqualTo(WorldTimeService.DayPhase.DUSK);

        world.setCurrentGameTime(atHour(22));
        assertThat(service.dayPhase(world)).isEqualTo(WorldTimeService.DayPhase.NIGHT);
    }

    @Test
    void advanceTimeShouldAdvanceGameTime() {
        var world = new World("test", UUID.randomUUID(), "{}");
        var start = Instant.now();
        world.setCurrentGameTime(start);

        when(worldRepo.save(any())).thenAnswer(inv -> inv.<World>getArgument(0));

        var result = service.advanceTime(world, Duration.ofMinutes(120));

        assertThat(result.getCurrentGameTime()).isAfter(start);
        verify(eventService).publish(any(), any(), any(), any(), any());
    }

    @Test
    void setTimeShouldSetToTargetTime() {
        var world = new World("test", UUID.randomUUID(), "{}");
        world.setCurrentGameTime(Instant.now());
        var target = Instant.now().plus(Duration.ofDays(1));

        when(worldRepo.save(any())).thenAnswer(inv -> inv.<World>getArgument(0));

        var result = service.setTime(world, target);

        assertThat(result.getCurrentGameTime()).isEqualTo(target);
        verify(eventService).publish(any(), any(), any(), any(), any());
    }

    private static Instant atHour(int hour) {
        return Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.of(hour, 0)).toInstant();
    }
}
