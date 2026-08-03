package com.lwe.api;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.User;
import com.lwe.core.service.CampaignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CreateRequest req,
                                                    @AuthenticationPrincipal User user) {
        var campaign = service.create(req.worldId(), req.gameSystemId(), req.name(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignResponse.from(campaign));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> list(@AuthenticationPrincipal User user) {
        var campaigns = service.listAccessible(user.getId()).stream().map(CampaignResponse::from).toList();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable UUID id,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CampaignResponse.from(service.getById(id, user.getId())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CampaignResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateRequest req,
                                                    @AuthenticationPrincipal User user) {
        var campaign = service.update(id, user.getId(), req.name(), req.stateJson());
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal User user) {
        service.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotNull UUID worldId,
        @NotNull UUID gameSystemId,
        @NotBlank String name
    ) {}

    public record UpdateRequest(String name, String stateJson) {}

    public record CampaignResponse(
        UUID id, UUID worldId, UUID gameSystemId, String name,
        String settingsJson, String stateJson, String createdAt, String updatedAt
    ) {
        static CampaignResponse from(Campaign c) {
            return new CampaignResponse(
                c.getId(), c.getWorldId(), c.getGameSystemId(), c.getName(),
                c.getSettingsJson(), c.getStateJson(),
                c.getCreatedAt().toString(), c.getUpdatedAt().toString());
        }
    }
}
