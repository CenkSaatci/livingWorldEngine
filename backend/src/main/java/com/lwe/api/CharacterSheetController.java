package com.lwe.api;

import com.lwe.api.dto.EntityResponse;
import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.CharacterSheetService;
import com.lwe.core.service.EntityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities")
public class CharacterSheetController {

    private final CharacterSheetService sheetService;
    private final EntityService entityService;

    public CharacterSheetController(CharacterSheetService sheetService,
                                    EntityService entityService) {
        this.sheetService = sheetService;
        this.entityService = entityService;
    }

    @GetMapping("/{entityId}")
    public ResponseEntity<EntityResponse> getEntity(@PathVariable UUID entityId,
                                                    @AuthenticationPrincipal User user) {
        var entity = entityService.getById(entityId, user.getId());
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @GetMapping("/{entityId}/sheet")
    public ResponseEntity<SheetResponse> getSheet(@PathVariable UUID entityId,
                                                    @RequestParam(required = false) UUID campaignId,
                                                    @AuthenticationPrincipal User user) {
        var sheet = sheetService.getSheet(entityId, user.getId(), campaignId);
        return ResponseEntity.ok(sheet);
    }

    @PatchMapping("/{entityId}/progression")
    public ResponseEntity<SheetResponse> updateProgression(@PathVariable UUID entityId,
                                                            @RequestBody Map<String, Object> body,
                                                            @AuthenticationPrincipal User user) {
        var xp = ((Number) body.getOrDefault("experience_points", 0)).intValue();
        sheetService.updateProgression(entityId, user.getId(), xp);
        var sheet = sheetService.getSheet(entityId, user.getId());
        return ResponseEntity.ok(sheet);
    }

    // Direkte PATCH-Endpunkte ohne worldId im Pfad (genutzt vom CharacterSheet);
    // die Welt wird aus der Entity aufgelöst, Zugriff via EntityService geprüft.
    @PatchMapping("/{entityId}/attributes")
    public ResponseEntity<EntityResponse> updateAttributes(@PathVariable UUID entityId,
                                                            @RequestBody Map<String, Integer> attrs,
                                                            @AuthenticationPrincipal User user) {
        var entity = entityService.updateAttributes(entityId, user.getId(), attrs);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/skills")
    public ResponseEntity<EntityResponse> updateSkills(@PathVariable UUID entityId,
                                                        @RequestBody Map<String, Integer> skills,
                                                        @RequestParam(required = false) UUID campaignId,
                                                        @AuthenticationPrincipal User user) {
        var entity = entityService.updateSkills(entityId, user.getId(), skills, campaignId);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PostMapping("/{entityId}/fate/spend")
    public ResponseEntity<EntityResponse> spendFatePoint(@PathVariable UUID entityId,
                                                          @RequestParam(required = false) UUID campaignId,
                                                          @AuthenticationPrincipal User user) {
        var entity = entityService.spendFatePoint(entityId, user.getId(), campaignId);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PostMapping("/{entityId}/conditions")
    public ResponseEntity<EntityResponse> addCondition(@PathVariable UUID entityId,
                                                        @RequestBody Map<String, Object> body,
                                                        @RequestParam(required = false) UUID campaignId,
                                                        @AuthenticationPrincipal User user) {
        var name = body.get("name") instanceof String s2 ? s2 : null;
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        var rounds = body.get("rounds") instanceof Number n ? n.intValue() : null;
        var entity = entityService.addCondition(entityId, user.getId(), name, rounds, campaignId);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @DeleteMapping("/{entityId}/conditions/{name}")
    public ResponseEntity<EntityResponse> removeCondition(@PathVariable UUID entityId,
                                                           @PathVariable String name,
                                                           @AuthenticationPrincipal User user) {
        var entity = entityService.removeCondition(entityId, user.getId(), name);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/override")
    public ResponseEntity<EntityResponse> updateOverride(@PathVariable UUID entityId,
                                                          @RequestBody Map<String, Object> body,
                                                          @AuthenticationPrincipal User user) {
        var entity = entityService.updateOverrides(entityId, user.getId(), body);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }
}
