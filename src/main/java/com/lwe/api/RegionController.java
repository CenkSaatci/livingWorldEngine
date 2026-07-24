package com.lwe.api;

import com.lwe.api.dto.RegionResponse;
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

import com.lwe.api.dto.RegionResponse;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worlds/{worldId}/regions")
public class RegionController {

    private final RegionService service;

    public RegionController(RegionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegionResponse> create(@PathVariable UUID worldId,
                                                  @Valid @RequestBody CreateRequest req,
                                                  @AuthenticationPrincipal User user) {
        var region = service.create(worldId, user.getId(), req.name(),
            req.description(), req.history(), req.dangerLevel(), req.climate(),
            req.resources(), req.factions(), req.positionJson(), req.polygonPoints());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegionResponse.from(region));
    }

    @GetMapping
    public ResponseEntity<List<RegionResponse>> list(@PathVariable UUID worldId,
                                                      @AuthenticationPrincipal User user) {
        var regions = service.list(worldId, user.getId()).stream().map(RegionResponse::from).toList();
        return ResponseEntity.ok(regions);
    }

    @GetMapping("/{regionId}")
    public ResponseEntity<RegionResponse> getById(@PathVariable UUID worldId,
                                                   @PathVariable UUID regionId,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(RegionResponse.from(service.getById(regionId, user.getId())));
    }

    @PatchMapping("/{regionId}")
    public ResponseEntity<RegionResponse> update(@PathVariable UUID worldId,
                                                  @PathVariable UUID regionId,
                                                  @RequestBody UpdateRequest req,
                                                  @AuthenticationPrincipal User user) {
        var region = service.update(regionId, user.getId(), req.name(),
            req.description(), req.history(), req.dangerLevel(), req.climate(),
            req.resources(), req.factions(), req.positionJson(), req.polygonPoints());
        return ResponseEntity.ok(RegionResponse.from(region));
    }

    @DeleteMapping("/{regionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID worldId,
                                        @PathVariable UUID regionId,
                                        @AuthenticationPrincipal User user) {
        service.delete(regionId, user.getId());
        return ResponseEntity.noContent().build();
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
