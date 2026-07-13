package com.lwe.api;

import com.lwe.core.service.GameSystemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/game-systems")
public class GameSystemController {

    private final GameSystemService service;

    public GameSystemController(GameSystemService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req) {
        var gs = service.create(req.name(), req.version(), req.rulesJson(), req.schemaJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", gs.getId(),
            "name", gs.getName(),
            "version", gs.getVersion(),
            "active", gs.isActive()
        ));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        var list = service.listActive().stream().map(gs -> Map.of(
            "id", gs.getId(),
            "name", gs.getName(),
            "version", gs.getVersion()
        )).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        var gs = service.getById(id);
        return ResponseEntity.ok(Map.of(
            "id", gs.getId(),
            "name", gs.getName(),
            "version", gs.getVersion(),
            "rules_json", gs.getRulesJson(),
            "active", gs.isActive()
        ));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validate(@PathVariable UUID id) {
        var errors = service.revalidate(id);
        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.ok(Map.of(
            "valid", false,
            "errors", errors
        ));
    }

    public record CreateRequest(
        @NotBlank String name,
        @Positive int version,
        @NotBlank String rulesJson,
        @NotBlank String schemaJson
    ) {}
}