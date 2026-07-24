package com.lwe.api;

import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.CharacterSheetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
