package com.lwe.api;

import com.lwe.api.dto.GameSystemInfoResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.GameSystemService;
import com.lwe.rules.RuleSchemaValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/game-systems")
public class GameSystemController {

    private final GameSystemService service;
    private final RuleSchemaValidator validator;

    public GameSystemController(GameSystemService service, RuleSchemaValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<GameSystemInfoResponse> create(@Valid @RequestBody CreateRequest req,
                                                         @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        var gs = service.create(req.name(), req.version(), req.rulesJson(), req.schemaJson(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(GameSystemInfoResponse.from(gs));
    }

    @GetMapping
    public ResponseEntity<List<GameSystemInfoResponse>> list(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        var list = service.listVisible(user.getId(), isAdmin(user))
            .stream().map(GameSystemInfoResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    private static boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        var gs = service.getReadable(id, user.getId(), isAdmin(user));
        return ResponseEntity.ok(new GameSystemDetailResponse(
            gs.getId(), gs.getName(), gs.getVersion(), gs.getRulesJson(), gs.isActive()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GameSystemInfoResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody CreateRequest req,
                                                          @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        var gs = service.update(id, req.name(), req.version(), req.rulesJson(), user.getId(), isAdmin(user));
        return ResponseEntity.ok(GameSystemInfoResponse.from(gs));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody String rulesJson) {
        // Frontend sendet den rulesJson-String im Body — StringHttpMessageConverter
        // liefert ihn hier roh an. Ein JSON-Wrapper-Objekt würde zu falscher Validierung führen.
        // Leeres Schema "{}" bedeutet: gegen das Default-Schema prüfen (wie beim Speichern).
        var errors = validator.validate(rulesJson, RuleSchemaValidator.DEFAULT_SCHEMA);
        if (errors.isEmpty()) return ResponseEntity.ok(new ValidationResponse(true, null));
        return ResponseEntity.ok(new ValidationResponse(false,
            errors.stream().map(e -> e.path() + ": " + e.message()).toList()));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<GameSystemInfoResponse> clone(@PathVariable UUID id,
                                                        @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        var gs = service.clone(id, user.getId(), isAdmin(user));
        return ResponseEntity.ok(GameSystemInfoResponse.from(gs));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        service.delete(id, user.getId(), isAdmin(user));
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(@NotBlank String name, @Positive int version, String rulesJson, String schemaJson) {}
    public record GameSystemDetailResponse(UUID id, String name, int version, String rulesJson, boolean active) {}
    public record ValidationResponse(boolean valid, List<String> errors) {}
}
