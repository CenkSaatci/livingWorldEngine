package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "region_weather")
public class RegionWeather {

    @Id
    private UUID regionId;

    @Column(name = "weather_type", nullable = false, length = 20)
    private String weatherType = "CLEAR";

    @Column(nullable = false)
    private int temperature = 15;

    @Column(nullable = false)
    private int wind = 0;

    @Column(length = 200)
    private String description;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected RegionWeather() {}

    public RegionWeather(UUID regionId) {
        this.regionId = regionId;
    }

    @PrePersist
    @PreUpdate
    void onUpdate() { this.changedAt = Instant.now(); }

    public UUID getRegionId() { return regionId; }
    public String getWeatherType() { return weatherType; }
    public void setWeatherType(String v) { this.weatherType = v; }
    public int getTemperature() { return temperature; }
    public void setTemperature(int v) { this.temperature = v; }
    public int getWind() { return wind; }
    public void setWind(int v) { this.wind = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Instant getChangedAt() { return changedAt; }
}
