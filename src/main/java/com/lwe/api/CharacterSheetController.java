package com.lwe.api;

import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.CharacterSheetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities")
public class CharacterSheetController {

    private final CharacterSheetService sheetService;

    public CharacterSheetController(CharacterSheetService sheetService) {
        this.sheetService = sheetService;
    }

    @GetMapping("/{entityId}/sheet")
    public ResponseEntity<SheetResponse> getSheet(@PathVariable UUID entityId,
                                                    @AuthenticationPrincipal User user) {
        var sheet = sheetService.getSheet(entityId, user.getId());
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
}
