package com.lwe.api;

import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.User;
import com.lwe.core.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/game-systems/{gameSystemId}/items")
    public ResponseEntity<ItemResponse> create(@PathVariable UUID gameSystemId,
                                                @Valid @RequestBody CreateRequest req,
                                                @AuthenticationPrincipal User user) {
        var item = service.create(gameSystemId, user.getId(), req.name(), req.type(),
            req.weight(), req.value(), req.bonusesJson(), req.metadataJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
    }

    @GetMapping("/api/v1/game-systems/{gameSystemId}/items")
    public ResponseEntity<List<ItemResponse>> listByGameSystem(@PathVariable UUID gameSystemId,
                                                               @AuthenticationPrincipal User user) {
        var items = service.listByGameSystem(gameSystemId).stream().map(ItemResponse::from).toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/api/v1/items/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable UUID id,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ItemResponse.from(service.getById(id)));
    }

    @PutMapping("/api/v1/items/{id}")
    public ResponseEntity<ItemResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateRequest req,
                                                @AuthenticationPrincipal User user) {
        var item = service.update(id, req.name(), req.type(), req.weight(),
            req.value(), req.bonusesJson(), req.metadataJson());
        return ResponseEntity.ok(ItemResponse.from(item));
    }

    @DeleteMapping("/api/v1/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal User user) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotBlank String name,
        @NotBlank String type,
        BigDecimal weight,
        int value,
        String bonusesJson,
        String metadataJson
    ) {}

    public record UpdateRequest(
        String name,
        String type,
        BigDecimal weight,
        Integer value,
        String bonusesJson,
        String metadataJson
    ) {}

    public record ItemResponse(
        UUID id, UUID gameSystemId, String name, String type,
        String weight, int value, String bonusesJson, String metadataJson
    ) {
        static ItemResponse from(GameItem i) {
            return new ItemResponse(
                i.getId(), i.getGameSystemId(), i.getName(), i.getType(),
                i.getWeight().toString(), i.getValue(),
                i.getBonusesJson(), i.getMetadataJson());
        }
    }
}
