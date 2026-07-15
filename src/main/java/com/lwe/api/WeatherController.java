package com.lwe.api;

import com.lwe.core.domain.RegionWeather;
import com.lwe.time.WeatherService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regions")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{regionId}/weather")
    public RegionWeather getWeather(@PathVariable UUID regionId) {
        var w = weatherService.getWeather(regionId);
        if (w == null) {
            // Fallback: leeres Weather-Objekt zurückgeben (wird beim nächsten Tick initialisiert)
            var fallback = new RegionWeather(regionId);
            fallback.setDescription("Wetter wird ermittelt…");
            return fallback;
        }
        return w;
    }
}
