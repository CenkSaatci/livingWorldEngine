package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.WorldMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WorldMapController {

    private final WorldMapService mapService;

    public WorldMapController(WorldMapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/worlds/{worldId}/map")
    public ResponseEntity<?> getMap(@PathVariable UUID worldId,
                                    @AuthenticationPrincipal User user) {
        var map = mapService.getMap(worldId, user.getId());
        if (map == null && mapService.canWrite(worldId, user.getId())) {
            map = mapService.getOrCreate(worldId, user.getId());
        }
        return map != null ? ResponseEntity.ok(map) : ResponseEntity.noContent().build();
    }

    @PatchMapping("/worlds/{worldId}/map")
    public ResponseEntity<?> updateMap(@PathVariable UUID worldId,
                                       @RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal User user) {
        var imageUrl = (String) body.get("image_url");
        var width = (Integer) body.get("width");
        var height = (Integer) body.get("height");
        var map = mapService.update(worldId, user.getId(), imageUrl, width, height);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/maps/{mapId}")
    public ResponseEntity<?> getMapById(@PathVariable UUID mapId,
                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mapService.getById(mapId, user.getId()));
    }
}
