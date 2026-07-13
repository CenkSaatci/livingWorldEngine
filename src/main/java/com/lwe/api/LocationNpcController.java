package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.LocationNpcService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations/{locationId}")
public class LocationNpcController {

    private final LocationNpcService service;

    public LocationNpcController(LocationNpcService service) {
        this.service = service;
    }

    @GetMapping("/npcs")
    public ResponseEntity<?> listNpcs(@PathVariable UUID locationId,
                                      @AuthenticationPrincipal User user) {
        var npcs = service.getNpcsAtLocation(locationId, user.getId()).stream()
            .map(e -> Map.of(
                "id", e.getId(),
                "name", e.getName(),
                "entity_type", e.getEntityType()
            ))
            .toList();
        return ResponseEntity.ok(npcs);
    }

    @GetMapping("/services")
    public ResponseEntity<?> listServices(@PathVariable UUID locationId,
                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getServices(locationId, user.getId()));
    }
}
