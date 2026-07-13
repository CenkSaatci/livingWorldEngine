package com.lwe.api;

import com.lwe.core.domain.Location;
import com.lwe.core.domain.User;
import com.lwe.core.service.LocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regions/{regionId}/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID regionId,
                                    @Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var loc = service.create(regionId, user.getId(), req.type(), req.name(),
            req.description(), req.history(), req.population(), req.wealth(),
            req.services(), req.factions(), req.isCapital(), req.positionJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(loc));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID regionId,
                                  @AuthenticationPrincipal User user) {
        var list = service.list(regionId, user.getId()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<?> getById(@PathVariable UUID regionId,
                                     @PathVariable UUID locationId,
                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(service.getById(locationId, user.getId())));
    }

    @PatchMapping("/{locationId}")
    public ResponseEntity<?> update(@PathVariable UUID regionId,
                                    @PathVariable UUID locationId,
                                    @RequestBody UpdateRequest req,
                                    @AuthenticationPrincipal User user) {
        var loc = service.update(locationId, user.getId(), req.type(), req.name(),
            req.description(), req.history(), req.population(), req.wealth(),
            req.services(), req.factions(), req.isCapital(), req.positionJson());
        return ResponseEntity.ok(toResponse(loc));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<?> delete(@PathVariable UUID regionId,
                                    @PathVariable UUID locationId,
                                    @AuthenticationPrincipal User user) {
        service.delete(locationId, user.getId());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Location l) {
        var m = new HashMap<String, Object>();
        m.put("id", l.getId());
        m.put("region_id", l.getRegionId());
        m.put("type", l.getType());
        m.put("name", l.getName());
        m.put("description", l.getDescription() != null ? l.getDescription() : "");
        m.put("history", l.getHistory() != null ? l.getHistory() : "");
        m.put("population", l.getPopulation());
        m.put("wealth", l.getWealth());
        m.put("services", l.getServices());
        m.put("factions", l.getFactions());
        m.put("is_capital", l.isCapital());
        m.put("created_at", l.getCreatedAt().toString());
        return m;
    }

    public record CreateRequest(
        @NotBlank String type, @NotBlank String name, String description, String history,
        int population, @Min(1) @Max(10) int wealth,
        String services, String factions, boolean isCapital, String positionJson
    ) {}
    public record UpdateRequest(
        String type, String name, String description, String history,
        Integer population, Integer wealth,
        String services, String factions, Boolean isCapital, String positionJson
    ) {}
}
