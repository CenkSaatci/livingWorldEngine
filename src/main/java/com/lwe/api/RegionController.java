package com.lwe.api;

import com.lwe.core.domain.Region;
import com.lwe.core.domain.User;
import com.lwe.core.service.RegionService;
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
@RequestMapping("/api/v1/worlds/{worldId}/regions")
public class RegionController {

    private final RegionService service;

    public RegionController(RegionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID worldId,
                                    @Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var region = service.create(worldId, user.getId(), req.name(),
            req.description(), req.history(), req.dangerLevel(), req.climate(),
            req.resources(), req.factions(), req.positionJson(), req.polygonPoints());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(region));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID worldId,
                                  @AuthenticationPrincipal User user) {
        var regions = service.list(worldId, user.getId()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(regions);
    }

    @GetMapping("/{regionId}")
    public ResponseEntity<?> getById(@PathVariable UUID worldId,
                                     @PathVariable UUID regionId,
                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(service.getById(regionId, user.getId())));
    }

    @PatchMapping("/{regionId}")
    public ResponseEntity<?> update(@PathVariable UUID worldId,
                                    @PathVariable UUID regionId,
                                    @RequestBody UpdateRequest req,
                                    @AuthenticationPrincipal User user) {
        var region = service.update(regionId, user.getId(), req.name(),
            req.description(), req.history(), req.dangerLevel(), req.climate(),
            req.resources(), req.factions(), req.positionJson(), req.polygonPoints());
        return ResponseEntity.ok(toResponse(region));
    }

    @DeleteMapping("/{regionId}")
    public ResponseEntity<?> delete(@PathVariable UUID worldId,
                                    @PathVariable UUID regionId,
                                    @AuthenticationPrincipal User user) {
        service.delete(regionId, user.getId());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Region r) {
        var m = new HashMap<String, Object>();
        m.put("id", r.getId());
        m.put("world_id", r.getWorldId());
        m.put("name", r.getName());
        m.put("description", r.getDescription() != null ? r.getDescription() : "");
        m.put("history", r.getHistory() != null ? r.getHistory() : "");
        m.put("danger_level", r.getDangerLevel());
        m.put("climate", r.getClimate());
        m.put("resources", r.getResources());
        m.put("factions", r.getFactions());
        m.put("population", r.getPopulation());
        m.put("polygon_points", r.getPolygonPoints() != null ? r.getPolygonPoints() : null);
        m.put("created_at", r.getCreatedAt().toString());
        return m;
    }

    public record CreateRequest(
        @NotBlank String name, String description, String history,
        @Min(1) @Max(10) int dangerLevel, String climate,
        String resources, String factions, String positionJson,
        String polygonPoints
    ) {}
    public record UpdateRequest(
        String name, String description, String history,
        Integer dangerLevel, String climate,
        String resources, String factions, String positionJson,
        String polygonPoints
    ) {}
}
