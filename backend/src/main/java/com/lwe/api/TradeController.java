package com.lwe.api;

import com.lwe.core.domain.Trade;
import com.lwe.core.domain.User;
import com.lwe.core.service.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeService tradeService;
    private final com.lwe.core.repository.GameItemRepository itemRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();

    public TradeController(TradeService tradeService,
                           com.lwe.core.repository.GameItemRepository itemRepo) {
        this.tradeService = tradeService;
        this.itemRepo = itemRepo;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> propose(@Valid @RequestBody ProposeRequest req,
                                                 @AuthenticationPrincipal User user) {
        var trade = tradeService.propose(req.worldId(), user.getId(), req.proposerEntityId(),
            req.partnerEntityId(), req.offer(), req.request());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(trade));
    }

    @PostMapping("/{id}/counter")
    public ResponseEntity<TradeResponse> counter(@PathVariable UUID id,
                                                 @Valid @RequestBody CounterRequest req,
                                                 @AuthenticationPrincipal User user) {
        var trade = tradeService.counter(id, user.getId(), req.entityId(), req.offer(), req.request());
        return ResponseEntity.ok(toResponse(trade));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<TradeResponse> accept(@PathVariable UUID id,
                                                @Valid @RequestBody ActorRequest req,
                                                @AuthenticationPrincipal User user) {
        var trade = tradeService.accept(id, user.getId(), req.entityId());
        return ResponseEntity.ok(toResponse(trade));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TradeResponse> cancel(@PathVariable UUID id,
                                                @Valid @RequestBody ActorRequest req,
                                                @AuthenticationPrincipal User user) {
        var trade = tradeService.cancel(id, user.getId(), req.entityId());
        return ResponseEntity.ok(toResponse(trade));
    }

    @GetMapping
    public ResponseEntity<List<TradeResponse>> list(@RequestParam UUID worldId,
                                                     @RequestParam UUID entityId,
                                                     @AuthenticationPrincipal User user) {
        var trades = tradeService.list(worldId, user.getId(), entityId);
        // Audit R4: Item-Namen in EINER Query fuer alle Trades laden.
        var ids = trades.stream()
            .flatMap(t -> java.util.stream.Stream.of(t.getOfferJson(), t.getRequestJson()))
            .flatMap(json -> itemIds(json).stream())
            .distinct()
            .toList();
        var names = new java.util.HashMap<UUID, String>();
        itemRepo.findAllById(ids).forEach(g -> names.put(g.getId(), g.getName()));
        return ResponseEntity.ok(trades.stream().map(t -> toResponse(t, names)).toList());
    }

    public record ProposeRequest(
        @NotNull UUID worldId,
        @NotNull UUID proposerEntityId,
        @NotNull UUID partnerEntityId,
        List<Map<String, Object>> offer,
        List<Map<String, Object>> request
    ) {}

    public record CounterRequest(
        @NotNull UUID entityId,
        List<Map<String, Object>> offer,
        List<Map<String, Object>> request
    ) {}

    public record ActorRequest(@NotNull UUID entityId) {}

    public record TradeResponse(
        UUID id, UUID worldId, UUID proposerEntityId, UUID partnerEntityId,
        String offerJson, String requestJson, String status, UUID lastEditorEntityId,
        String updatedAt,
        java.util.List<java.util.Map<String, Object>> offer,
        java.util.List<java.util.Map<String, Object>> request
    ) {}

    private TradeResponse toResponse(Trade t) {
        var names = new java.util.HashMap<UUID, String>();
        var ids = java.util.stream.Stream.of(t.getOfferJson(), t.getRequestJson())
            .flatMap(json -> itemIds(json).stream()).distinct().toList();
        itemRepo.findAllById(ids).forEach(g -> names.put(g.getId(), g.getName()));
        return toResponse(t, names);
    }

    private TradeResponse toResponse(Trade t, java.util.Map<UUID, String> names) {
        return new TradeResponse(t.getId(), t.getWorldId(), t.getProposerEntityId(),
            t.getPartnerEntityId(), t.getOfferJson(), t.getRequestJson(),
            t.getStatus(), t.getLastEditorEntityId(), t.getUpdatedAt().toString(),
            enrich(t.getOfferJson(), names), enrich(t.getRequestJson(), names));
    }

    private java.util.List<UUID> itemIds(String json) {
        try {
            java.util.List<java.util.Map<String, Object>> items =
                objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            return items.stream()
                .map(i -> UUID.fromString(String.valueOf(i.get("itemId"))))
                .distinct().toList();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    /** R3/R4: Item-Namen aus vorab geladener Map anreichern (kein N+1). */
    private java.util.List<java.util.Map<String, Object>> enrich(
            String json, java.util.Map<UUID, String> names) {
        try {
            java.util.List<java.util.Map<String, Object>> items =
                objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            for (var item : items) {
                var id = UUID.fromString(String.valueOf(item.get("itemId")));
                item.put("name", names.getOrDefault(id, id.toString()));
            }
            return items;
        } catch (Exception e) {
            return java.util.List.of();
        }
    }
}
