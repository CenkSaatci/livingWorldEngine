package com.lwe.api;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.CampaignMember;
import com.lwe.core.domain.User;
import com.lwe.core.service.CampaignMemberService;
import com.lwe.core.service.CampaignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService service;
    private final CampaignMemberService memberService;

    public CampaignController(CampaignService service, CampaignMemberService memberService) {
        this.service = service;
        this.memberService = memberService;
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
        var campaign = service.update(id, user.getId(), req.name(), req.stateJson(), req.settingsJson());
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal User user) {
        service.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<CampaignMemberResponse> addMember(@PathVariable UUID id,
                                                             @Valid @RequestBody MemberRequest req,
                                                             @AuthenticationPrincipal User user) {
        var member = memberService.addMember(id, user.getId(), req.userId(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignMemberResponse.from(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CampaignMemberResponse>> listMembers(@PathVariable UUID id,
                                                                     @AuthenticationPrincipal User user) {
        var members = memberService.listMembers(id, user.getId());
        return ResponseEntity.ok(members.stream().map(CampaignMemberResponse::from).toList());
    }

    @PostMapping("/{id}/pull-system")
    public ResponseEntity<CampaignResponse> pullSystem(@PathVariable UUID id,
                                                        @AuthenticationPrincipal User user) {
        var campaign = service.pullSystem(id, user.getId());
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @PatchMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> updateMemberRole(@PathVariable UUID id,
                                                  @PathVariable UUID memberId,
                                                  @RequestBody java.util.Map<String, Object> body,
                                                  @AuthenticationPrincipal User user) {
        var role = body.get("role") instanceof String s2 ? s2 : null;
        memberService.updateRole(id, user.getId(), memberId, role);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID memberId,
                                              @AuthenticationPrincipal User user) {
        memberService.removeMember(id, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotNull UUID worldId,
        @NotNull UUID gameSystemId,
        @NotBlank String name
    ) {}

    public record UpdateRequest(String name, String stateJson, String settingsJson) {}

    public record MemberRequest(@NotNull UUID userId, @NotBlank String role) {}

    public record CampaignMemberResponse(
        UUID id, UUID campaignId, UUID userId, String role, String joinedAt
    ) {
        static CampaignMemberResponse from(CampaignMember m) {
            return new CampaignMemberResponse(
                m.getId(), m.getCampaignId(), m.getUserId(), m.getRole(), m.getJoinedAt().toString());
        }
    }

    public record CampaignResponse(
        UUID id, UUID worldId, UUID gameSystemId, String name,
        String settingsJson, String stateJson, String createdAt, String updatedAt,
        Integer gameSystemVersion
    ) {
        static CampaignResponse from(Campaign c) {
            return new CampaignResponse(
                c.getId(), c.getWorldId(), c.getGameSystemId(), c.getName(),
                c.getSettingsJson(), c.getStateJson(),
                c.getCreatedAt().toString(), c.getUpdatedAt().toString(),
                c.getGameSystemVersion());
        }
    }
}
