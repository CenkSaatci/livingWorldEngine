package com.lwe.time;

import com.lwe.core.domain.Region;
import com.lwe.core.domain.RegionWeather;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.RegionWeatherRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.EntityEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Weather Engine — erzeugt und rotiert Wetter pro Region.
 *
 * <p>Weather wird auf drei Wegen aktualisiert:
 * <ol>
 *   <li><b>Time-Tick</b>: alle realen 60s prüfen, ob ein Wetterwechsel ansteht</li>
 *   <li><b>DM-Kommando</b>: manuelles Setzen via REST</li>
 *   <li><b>Klima-abhängig</b>: Wüste bekommt nie Schnee, Dschungel nie Frost</li>
 * </ol>
 *
 * <p>Jeder Wetterwechsel erzeugt ein Entity-Event für die betroffene Region
 * und ein World-Event für den WebSocket-Broadcast.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WorldRepository worldRepo;
    private final RegionRepository regionRepo;
    private final RegionWeatherRepository weatherRepo;
    private final EntityEventService entityEventService;
    private final RandomGenerator rng = RandomGenerator.getDefault();

    private static final Map<String, List<String>> CLIMATE_WEATHERS = Map.of(
        "desert", List.of("CLEAR", "EXTREME_HEAT", "CLEAR", "CLEAR"),
        "tundra", List.of("CLEAR", "SNOW", "SNOW", "FOG"),
        "jungle", List.of("RAIN", "RAIN", "STORM", "FOG"),
        "swamp", List.of("FOG", "RAIN", "CLOUDY", "CLOUDY"),
        "mountains", List.of("CLEAR", "CLOUDY", "SNOW", "STORM"),
        "coast", List.of("CLEAR", "CLOUDY", "RAIN", "STORM"),
        "forest", List.of("CLEAR", "CLOUDY", "RAIN", "FOG"),
        "plains", List.of("CLEAR", "CLOUDY", "RAIN", "WINDY"),
        "arctic", List.of("SNOW", "SNOW", "STORM", "CLEAR")
    );

    private static final Map<String, String> WEATHER_DESC_DE = Map.of(
        "CLEAR", "Klarer Himmel",
        "CLOUDY", "Bewölkt",
        "RAIN", "Regen",
        "STORM", "Gewitter",
        "FOG", "Nebel",
        "SNOW", "Schnee",
        "WINDY", "Starker Wind",
        "EXTREME_HEAT", "Extreme Hitze"
    );

    public WeatherService(WorldRepository worldRepo, RegionRepository regionRepo,
                          RegionWeatherRepository weatherRepo,
                          EntityEventService entityEventService) {
        this.worldRepo = worldRepo;
        this.regionRepo = regionRepo;
        this.weatherRepo = weatherRepo;
        this.entityEventService = entityEventService;
    }

    /**
     * Wetter-Tick: läuft alle 60s und wechselt Wetter mit ~33% Wahrscheinlichkeit.
     */
    @Scheduled(fixedDelay = 60_000)
    public void tickAllWeather() {
        var worlds = worldRepo.findByActiveTrue();
        for (var world : worlds) {
            var regions = regionRepo.findByWorldIdOrderByNameAsc(world.getId());
            for (var region : regions) {
                if (rng.nextDouble() > 0.33) continue; // ~33% Wechsel-Chance
                try {
                    rollWeather(region);
                } catch (Exception e) {
                    log.warn("Weather roll failed for region {}: {}", region.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Erzeugt neues Wetter für die Region basierend auf Klima und Zufall.
     */
    @Transactional
    public RegionWeather rollWeather(Region region) {
        int temp = baseTemperature(region.getClimate());
        int wind = rng.nextInt(0, 7);
        String weather = randomWeatherForClimate(region.getClimate());

        // Temperatur an Wetter anpassen
        switch (weather) {
            case "SNOW" -> temp -= 10;
            case "EXTREME_HEAT" -> temp += 15;
            case "RAIN" -> temp -= 3;
            case "STORM" -> { temp -= 5; wind += 3; }
            case "FOG" -> wind = Math.max(0, wind - 2);
        }
        temp = Math.clamp(temp, -20, 50);
        wind = Math.clamp(wind, 0, 10);

        var rw = weatherRepo.findById(region.getId())
            .orElse(new RegionWeather(region.getId()));
        rw.setWeatherType(weather);
        rw.setTemperature(temp);
        rw.setWind(wind);
        rw.setDescription(WEATHER_DESC_DE.getOrDefault(weather, weather));
        rw = weatherRepo.save(rw);

        // Entity-Event für die Region
        entityEventService.publish("region", region.getId(),
            "WEATHER_CHANGED", "Wetterwechsel",
            "Wetter in " + region.getName() + ": " + rw.getDescription()
                + " (" + temp + "°C, Wind " + wind + "/10)",
            1, null);

        return rw;
    }

    public RegionWeather getWeather(UUID regionId) {
        return weatherRepo.findById(regionId)
            .orElseThrow(() -> new RuntimeException("WEATHER_NOT_FOUND"));
    }

    public Map<UUID, RegionWeather> getWeatherForRegionIds(List<UUID> regionIds) {
        var list = weatherRepo.findAllByRegionIdIn(regionIds);
        var map = new HashMap<UUID, RegionWeather>();
        for (var w : list) map.put(w.getRegionId(), w);
        return map;
    }

    // -- Hilfsmethoden --

    private String randomWeatherForClimate(String climate) {
        var pool = CLIMATE_WEATHERS.getOrDefault(climate,
            List.of("CLEAR", "CLOUDY", "RAIN"));
        return pool.get(rng.nextInt(pool.size()));
    }

    private int baseTemperature(String climate) {
        return switch (climate) {
            case "desert" -> 35;
            case "tundra", "arctic" -> -5;
            case "jungle" -> 28;
            case "mountains" -> 5;
            case "swamp" -> 20;
            case "coast" -> 18;
            default -> 15; // temperate, forest, plains
        };
    }
}
