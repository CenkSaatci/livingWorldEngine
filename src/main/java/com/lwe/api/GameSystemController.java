package com.lwe.api;

import com.lwe.api.dto.GameSystemInfoResponse;
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
    public ResponseEntity<GameSystemInfoResponse> create(@Valid @RequestBody CreateRequest req) {
        var gs = service.create(req.name(), req.version(), req.rulesJson(), req.schemaJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(GameSystemInfoResponse.from(gs));
    }

    @GetMapping
    public ResponseEntity<List<GameSystemInfoResponse>> list() {
        var list = service.listActive().stream().map(GameSystemInfoResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        var gs = service.getById(id);
        return ResponseEntity.ok(new GameSystemDetailResponse(
            gs.getId(), gs.getName(), gs.getVersion(), gs.getRulesJson(), gs.isActive()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameSystemInfoResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody CreateRequest req) {
        var gs = service.update(id, req.name(), req.version(), req.rulesJson());
        return ResponseEntity.ok(GameSystemInfoResponse.from(gs));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody String rulesJson) {
        var errors = validator.validate(rulesJson, "{}");
        if (errors.isEmpty()) return ResponseEntity.ok(new ValidationResponse(true, null));
        return ResponseEntity.ok(new ValidationResponse(false,
            errors.stream().map(e -> e.path() + ": " + e.message()).toList()));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<GameSystemInfoResponse> clone(@PathVariable UUID id) {
        var gs = service.clone(id);
        return ResponseEntity.ok(GameSystemInfoResponse.from(gs));
    }

    public record CreateRequest(@NotBlank String name, @Positive int version, String rulesJson, String schemaJson) {}
    public record GameSystemDetailResponse(UUID id, String name, int version, String rulesJson, boolean active) {}
    public record ValidationResponse(boolean valid, List<String> errors) {}
}
