package com.lwe.time;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.World;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.WorldEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;

import static com.lwe.core.service.WorldEventService.EventType.TIME_ADVANCED;

/**
 * Time Engine — steuert die In-Game-Zeit aller Welten gemäß {@code settings_json.time}.
 *
 * <p>Drei Modi (siehe {@code docs/ADR/009-world-time-calendar-system.md}):
 * <ul>
 *   <li>{@code automatic} — Zeit tickt automatisch im {@code tickIntervalRealSeconds}-Rhythmus</li>
 *   <li>{@code manual} — Zeit steht still, nur DM via {@code POST /time/advance|set}</li>
 *   <li>{@code hybrid} — Zeit tickt automatisch, DM kann manuell eingreifen</li>
 * </ul>
 */
@Service
public class WorldTimeService {

    private final WorldRepository worldRepo;
    private final WorldEventService eventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorldTimeService(WorldRepository worldRepo, WorldEventService eventService) {
        this.worldRepo = worldRepo;
        this.eventService = eventService;
    }

    /**
     * Tickt alle aktiven und automatisch laufenden Welten.
     * Läuft jede Sekunde, prüft aber pro Welt das Intervall.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void tickAllWorlds() {
        var worlds = worldRepo.findByActiveTrue();
        var now = Instant.now();

        for (var world : worlds) {
            var timeCfg = readTimeConfig(world);
            if (timeCfg == null) continue;
            if (timeCfg.paused()) continue;
            if ("manual".equals(timeCfg.mode())) continue;

            var lastTick = world.getLastTickAt();
            if (lastTick == null) {
                // Erster Tick — nur lastTickAt setzen
                world.setLastTickAt(now);
                worldRepo.save(world);
                continue;
            }

            var elapsed = Duration.between(lastTick, now).getSeconds();
            if (elapsed < timeCfg.tickIntervalRealSeconds()) continue;

            // Tick ausführen
            advanceWorld(world, timeCfg.tickAdvanceGameMinutes());
            world.setLastTickAt(now);
            worldRepo.save(world);
        }
    }

    @Transactional
    public World advanceTime(World world, Duration duration) {
        return advanceWorld(world, duration.toMinutes());
    }

    @Transactional
    public World setTime(World world, Instant targetTime) {
        world.setCurrentGameTime(targetTime);
        world = worldRepo.save(world);
        eventService.publish(world.getId(), TIME_ADVANCED, null, null, Map.of(
            "from", world.getCurrentGameTime() != null ? world.getCurrentGameTime().toString() : "",
            "to", targetTime.toString(),
            "trigger", "dm_set"
        ));
        return world;
    }

    public DayPhase dayPhase(World world) {
        var time = world.getCurrentGameTime();
        if (time == null) return DayPhase.DAY;
        var cfg = readTimeConfig(world);
        int dawnHour = cfg != null ? cfg.dayStartsAtHour() : 6;
        int hour = LocalTime.from(time.atZone(java.time.ZoneOffset.UTC)).getHour();
        return DayPhase.fromHour(hour, dawnHour);
    }

    private World advanceWorld(World world, long gameMinutes) {
        var current = world.getCurrentGameTime();
        if (current == null) {
            world.setCurrentGameTime(Instant.now());
            return worldRepo.save(world);
        }
        var newTime = current.plus(Duration.ofMinutes(gameMinutes));
        world.setCurrentGameTime(newTime);
        world = worldRepo.save(world);

        // Event nur bei signifikanten Sprüngen (>30 Spielminuten)
        if (gameMinutes >= 30) {
            eventService.publish(world.getId(), TIME_ADVANCED, null, null, Map.of(
                "from", current.toString(),
                "to", newTime.toString(),
                "byMin", gameMinutes,
                "trigger", "tick",
                "dayPhase", dayPhase(world).name()
            ));
        }
        return world;
    }

    public TimeConfig readTimeConfig(World world) {
        try {
            var tree = objectMapper.readTree(world.getSettingsJson());
            var time = tree.path("time");
            if (time.isMissingNode()) return null;
            return new TimeConfig(
                time.path("mode").asText("hybrid"),
                time.path("tick_interval_real_seconds").asInt(60),
                time.path("tick_advance_game_minutes").asInt(60),
                time.path("paused").asBoolean(false),
                time.path("day_starts_at_hour").asInt(6)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public record TimeConfig(String mode, int tickIntervalRealSeconds,
                              int tickAdvanceGameMinutes, boolean paused, int dayStartsAtHour) {
        public TimeConfig withPaused(boolean paused) {
            return new TimeConfig(mode, tickIntervalRealSeconds, tickAdvanceGameMinutes, paused, dayStartsAtHour);
        }
        public TimeConfig withMode(String mode) {
            return new TimeConfig(mode, tickIntervalRealSeconds, tickAdvanceGameMinutes, paused, dayStartsAtHour);
        }
    }

    public enum DayPhase { DAWN, DAY, DUSK, NIGHT;

        public static DayPhase fromHour(int hour, int dawnStart) {
            if (hour >= dawnStart - 1 && hour < dawnStart) return DAWN;
            if (hour >= dawnStart && hour < dawnStart + 11) return DAY;
            if (hour >= dawnStart + 11 && hour < dawnStart + 12) return DUSK;
            return NIGHT;
        }
    }
}