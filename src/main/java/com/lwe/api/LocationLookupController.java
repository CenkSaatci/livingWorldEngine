package com.lwe.api;

import com.lwe.api.dto.LocationLookupResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LocationLookupController {

    private final LocationService locationService;

    public LocationLookupController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/locations/{locationId}")
    public ResponseEntity<LocationLookupResponse> getLocation(@PathVariable UUID locationId,
                                                               @AuthenticationPrincipal User user) {
        var loc = locationService.getById(locationId, user.getId());
        return ResponseEntity.ok(new LocationLookupResponse(
            loc.getId(), loc.getName(), loc.getType(), loc.getRegionId()));
    }
}
