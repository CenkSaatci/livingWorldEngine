package com.lwe.core.repository;

import com.lwe.core.domain.RegionWeather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegionWeatherRepository extends JpaRepository<RegionWeather, UUID> {
    List<RegionWeather> findAllByRegionIdIn(List<UUID> regionIds);
    List<RegionWeather> findByRegionIdIn(List<UUID> regionIds);
}
