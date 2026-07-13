package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.repository.WorldRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * DM-Only-Endpunkte für Fog-of-War-Steuerung.
 */
@RestController
@RequestMapping("/api/v1/fog")
public class FogController {

    private final WorldRepository worldRepo;

    public FogController(WorldRepository worldRepo) {
        this.worldRepo = worldRepo;
    }

    @PatchMapping("/{worldId}")
    public ResponseEntity<?> updateFog(@PathVariable UUID worldId,
                                       @RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal User user) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new RuntimeException("WORLD_NOT_FOUND"));
        if (!world.getOwnerId().equals(user.getId()))
            return ResponseEntity.status(403).body(Map.of("error", "DM only"));

        // In einer späteren Phase: Persistenz in maps.fog_state_json
        // Aktuell nur Bestätigung — Fog-Verwaltung erfolgt client-seitig via WS
        return ResponseEntity.ok(Map.of("updated", true, "world_id", worldId));
    }

    @GetMapping("/{worldId}")
    public ResponseEntity<?> getFogState(@PathVariable UUID worldId,
                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("fog_active", true));
    }
}
