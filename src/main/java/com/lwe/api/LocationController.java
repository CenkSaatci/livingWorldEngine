package com.lwe.api;

import com.lwe.api.dto.LocationResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regions/{regionId}/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(@PathVariable UUID regionId,
                                                    @Valid @RequestBody CreateRequest req,
                                                    @AuthenticationPrincipal User user) {
        var loc = service.create(regionId, user.getId(), req.type(), req.name(),
            req.description(), req.history(), req.population(), req.wealth(),
            req.services(), req.factions(), req.isCapital(), req.positionJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(LocationResponse.from(loc));
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> list(@PathVariable UUID regionId,
                                                        @AuthenticationPrincipal User user) {
        var list = service.list(regionId, user.getId())
            .stream().map(LocationResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<LocationResponse> getById(@PathVariable UUID regionId,
                                                     @PathVariable UUID locationId,
                                                     @AuthenticationPrincipal User user) {
        var loc = service.getById(locationId, user.getId());
        return ResponseEntity.ok(LocationResponse.from(loc));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID regionId,
                                        @PathVariable UUID locationId,
                                        @AuthenticationPrincipal User user) {
        service.delete(locationId, user.getId());
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotBlank String type, @NotBlank String name, String description, String history,
        @Min(0) int population, @Min(1) @Max(10) int wealth,
        String services, String factions, boolean isCapital, String positionJson
    ) {}
}
