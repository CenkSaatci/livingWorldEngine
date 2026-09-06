package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.FogStatusResponse;
import com.lwe.api.dto.UpdatedResponse;
import com.lwe.core.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fog")
public class FogController {

    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestParam UUID worldId,
                                    @AuthenticationPrincipal User user) {
        if (!"ADMIN".equals(user.getRole()))
            return ResponseEntity.status(403).body(new ErrorResponse("DM only"));
        return ResponseEntity.ok(new UpdatedResponse(true, worldId));
    }

    @GetMapping("/status")
    public ResponseEntity<FogStatusResponse> status(@RequestParam UUID worldId) {
        return ResponseEntity.ok(new FogStatusResponse(true));
    }
}
