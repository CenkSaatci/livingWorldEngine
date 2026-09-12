package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Trade;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.repository.TradeRepository;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Handel zwischen Charakteren (B4): Angebot, Gegenangebot, Annahme, Abbruch.
 * Die Annahme tauscht atomar (eine Transaktion); Mengen werden beidseitig
 * erst bei Annahme erneut geprüft.
 */
@Service
public class TradeService {

    private static final TypeReference<List<Map<String, Object>>> ITEM_LIST = new TypeReference<>() {};

    private final TradeRepository tradeRepo;
    private final GameEntityRepository entityRepo;
    private final GameItemRepository itemRepo;
    private final InventoryService inventoryService;
    private final WorldAccess worldAccess;
    private final EntityAccess entityAccess;
    private final ObjectMapper objectMapper;

    public TradeService(TradeRepository tradeRepo, GameEntityRepository entityRepo,
                        GameItemRepository itemRepo, InventoryService inventoryService,
                        WorldAccess worldAccess, EntityAccess entityAccess, ObjectMapper objectMapper) {
        this.tradeRepo = tradeRepo;
        this.entityRepo = entityRepo;
        this.itemRepo = itemRepo;
        this.inventoryService = inventoryService;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Trade propose(UUID worldId, UUID userId, UUID proposerEntityId, UUID partnerEntityId,
                         List<Map<String, Object>> offer, List<Map<String, Object>> request) {
        var proposer = entityAccess.requireControl(proposerEntityId, userId, worldId); // Runde 1: F1
        var partner = entityOf(partnerEntityId, userId, worldId);
        if (proposer.getId().equals(partner.getId()))
            throw new TradeException("TRADE_SELF", "Cannot trade with yourself");
        var offerJson = checkItems(proposer, offer, "offer");
        var requestJson = checkItems(partner, request, "request");
        var trade = new Trade(worldId, proposer.getId(), partner.getId(), offerJson, requestJson);
        return tradeRepo.save(trade);
    }

    @Transactional
    public Trade counter(UUID tradeId, UUID userId, UUID editorEntityId,
                         List<Map<String, Object>> offer, List<Map<String, Object>> request) {
        var trade = getProposed(tradeId, userId);
        var editor = entityAccess.requireControl(editorEntityId, userId, trade.getWorldId()); // Runde 1: F1
        requireParticipant(trade, editor.getId());
        // Angebot gehört dem Editor, Wunsch dem Gegenüber.
        var other = entityOf(otherSide(trade, editor.getId()), userId, trade.getWorldId());
        trade.setOfferJson(checkItems(editor, offer, "offer"));
        trade.setRequestJson(checkItems(other, request, "request"));
        trade.setLastEditorEntityId(editor.getId());
        trade.touch();
        return tradeRepo.save(trade);
    }

    @Transactional
    public Trade accept(UUID tradeId, UUID userId, UUID acceptorEntityId) {
        var trade = getProposed(tradeId, userId);
        var acceptor = entityAccess.requireControl(acceptorEntityId, userId, trade.getWorldId()); // Runde 1: F1
        requireParticipant(trade, acceptor.getId());
        if (trade.getLastEditorEntityId().equals(acceptor.getId()))
            throw new TradeException("TRADE_SELF_ACCEPT", "Cannot accept your own offer");
        // F6: beide Entities in ID-sortierter Reihenfolge sperren (kein Deadlock, kein Doppel-Tausch).
        var ids = new java.util.ArrayList<>(List.of(trade.getProposerEntityId(), trade.getPartnerEntityId()));
        ids.sort(java.util.Comparator.naturalOrder());
        var proposer = lockedEntity(ids.get(0));
        var partner = lockedEntity(ids.get(1));
        // offer/request gehören immer dem letzten Editor (Counter schreibt aus Editor-Sicht).
        var from = trade.getLastEditorEntityId().equals(proposer.getId()) ? proposer : partner;
        var to = trade.getLastEditorEntityId().equals(proposer.getId()) ? partner : proposer;
        // Atomarer Tausch: erst beide Seiten abziehen (Mengenfehler -> Rollback), dann gutschreiben.
        moveItems(from, to, parseItems(trade.getOfferJson()), userId);
        moveItems(to, from, parseItems(trade.getRequestJson()), userId);
        trade.setStatus("accepted");
        trade.touch();
        return tradeRepo.save(trade);
    }

    @Transactional
    public Trade cancel(UUID tradeId, UUID userId, UUID entityId) {
        var trade = getProposed(tradeId, userId);
        var actor = entityOf(entityId, userId, trade.getWorldId());
        requireParticipant(trade, actor.getId());
        trade.setStatus("cancelled");
        trade.touch();
        return tradeRepo.save(trade);
    }

    @Transactional(readOnly = true)
    public List<Trade> list(UUID worldId, UUID userId, UUID entityId) {
        var entity = entityOf(entityId, userId, worldId);
        return tradeRepo.findByWorldAndEntity(worldId, entity.getId());
    }

    private void moveItems(GameEntity from, GameEntity to, List<Map<String, Object>> items, UUID userId) {
        for (var item : items) {
            var itemId = UUID.fromString(String.valueOf(item.get("itemId")));
            var qty = ((Number) item.get("quantity")).intValue();
            inventoryService.removeItem(from.getId(), userId, itemId, qty);
            inventoryService.addItem(to.getId(), userId, itemId, qty);
        }
    }

    /** F6: Trade mit Row-Lock laden (Doppel-Accept/-Counter ausgeschlossen). */
    private Trade getProposed(UUID tradeId, UUID userId) {
        var trade = tradeRepo.findByIdForUpdate(tradeId)
            .orElseThrow(() -> new TradeException("TRADE_NOT_FOUND", "Trade not found"));
        worldAccess.requireAccess(trade.getWorldId(), userId);
        if (!"proposed".equals(trade.getStatus()))
            throw new TradeException("TRADE_NOT_PROPOSED", "Trade is no longer open");
        return trade;
    }

    private void requireParticipant(Trade trade, UUID entityId) {
        if (!trade.getProposerEntityId().equals(entityId)
            && !trade.getPartnerEntityId().equals(entityId))
            throw new TradeException("TRADE_NOT_PARTICIPANT", "Not a participant");
    }

    private UUID otherSide(Trade trade, UUID entityId) {
        return trade.getProposerEntityId().equals(entityId)
            ? trade.getPartnerEntityId() : trade.getProposerEntityId();
    }

    private GameEntity lockedEntity(UUID entityId) {
        return entityRepo.findByIdForUpdate(entityId)
            .orElseThrow(() -> new TradeException("TRADE_ENTITY_GONE", "Entity no longer exists"));
    }

    private GameEntity entityOf(UUID entityId, UUID userId, UUID worldId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new TradeException("TRADE_ENTITY_NOT_FOUND", "Entity not found"));
        if (!entity.getWorldId().equals(worldId))
            throw new TradeException("TRADE_WORLD_MISMATCH", "Entity is not in this world");
        worldAccess.requireAccess(worldId, userId);
        return entity;
    }

    /** Validiert Items (Existenz + Bestand beim Besitzer) und liefert kanonisches JSON. */
    private String checkItems(GameEntity owner, List<Map<String, Object>> items, String side) {
        var clean = new ArrayList<Map<String, Object>>();
        for (var item : items == null ? List.<Map<String, Object>>of() : items) {
            var itemId = UUID.fromString(String.valueOf(item.get("itemId")));
            var qty = item.get("quantity") instanceof Number n ? n.intValue() : 0;
            if (qty < 1)
                throw new TradeException("TRADE_INVALID_QUANTITY", "Quantity must be >= 1 (" + side + ")");
            itemRepo.findById(itemId).orElseThrow(
                () -> new TradeException("TRADE_ITEM_NOT_FOUND", "Item not found"));
            var have = inventoryQuantity(owner, itemId);
            if (have < qty)
                throw new TradeException("TRADE_INSUFFICIENT_QUANTITY",
                    owner.getName() + " has only " + have + "x");
            var row = new LinkedHashMap<String, Object>();
            row.put("itemId", itemId.toString());
            row.put("quantity", qty);
            clean.add(row);
        }
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize trade items", e);
        }
    }

    private int inventoryQuantity(GameEntity owner, UUID itemId) {
        try {
            var raw = owner.getInventoryJson();
            if (raw == null || raw.isBlank()) return 0;
            List<Map<String, Object>> inv = objectMapper.readValue(raw, ITEM_LIST);
            return inv.stream()
                .filter(e -> itemId.toString().equals(String.valueOf(e.get("itemId"))))
                .mapToInt(e -> e.get("quantity") instanceof Number n ? n.intValue() : 0)
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, Object>> parseItems(String json) {
        try {
            return objectMapper.readValue(json, ITEM_LIST);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse trade items", e);
        }
    }

    public static class TradeException extends RuntimeException {
        private final String errorCode;
        public TradeException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
