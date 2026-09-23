package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.RuleNames;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Händler (ADR-015): NPCs mit {@code metadataJson.is_merchant} + {@code shop_inventory}.
 * Preise kommen aus der {@link EconomyService}-Formel (Wohlstand × {@code price_modifier});
 * ein explizit gesetzter {@code price} im Sortiment ist der feste Endpreis.
 *
 * <p>Kauf/Verkauf sind atomar: erst validieren (Geld/Bestand/Sortiment), dann Geld und
 * Item in einer Transaktion bewegen.
 */
@Service
public class MerchantService {

    /** H-2: harte Obergrenze gegen Overflow/Wirtschaftsexploit. */
    static final int MAX_QTY = 10_000;

    private final LocationRepository locationRepo;
    private final RegionRepository regionRepo;
    private final CampaignRepository campaignRepo;
    private final GameEntityRepository entityRepo;
    private final GameItemRepository itemRepo;
    private final RulesLoader rulesLoader;
    private final WorldAccess worldAccess;
    private final EntityAccess entityAccess;
    private final InventoryService inventoryService;
    private final CurrencyService currencyService;
    private final EconomyService economyService;
    private final PoiBindings bindings;
    private final ObjectMapper mapper;

    public MerchantService(LocationRepository locationRepo, RegionRepository regionRepo,
                           CampaignRepository campaignRepo, GameEntityRepository entityRepo,
                           GameItemRepository itemRepo, RulesLoader rulesLoader,
                           WorldAccess worldAccess, EntityAccess entityAccess,
                           InventoryService inventoryService, CurrencyService currencyService,
                           EconomyService economyService, PoiBindings bindings, ObjectMapper mapper) {
        this.locationRepo = locationRepo;
        this.regionRepo = regionRepo;
        this.campaignRepo = campaignRepo;
        this.entityRepo = entityRepo;
        this.itemRepo = itemRepo;
        this.rulesLoader = rulesLoader;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
        this.inventoryService = inventoryService;
        this.currencyService = currencyService;
        this.economyService = economyService;
        this.bindings = bindings;
        this.mapper = mapper;
    }

    public record Offer(String item, String itemId, int price, int sellPrice, boolean resolved) {}
    public record MerchantInfo(String npcId, String name, String occupation, String greeting,
                               double sellRate, boolean buyEnabled, boolean sellEnabled,
                               List<Offer> offers) {}
    public record TradeResult(String npcId, String item, int qty, int unitPrice, int total,
                              int moneyBefore, int moneyAfter,
                              String moneyBeforeText, String moneyAfterText) {}

    public List<MerchantInfo> list(UUID locationId, UUID userId, UUID campaignId) {
        var loc = location(locationId, userId);
        var worldId = worldIdOf(loc);
        var resolvedCampaign = resolveCampaign(worldId, campaignId);
        var rules = rulesLoader.loadRules(resolvedCampaign, worldId);
        var system = rulesLoader.resolveSystem(resolvedCampaign, worldId);
        var items = itemsOf(system);
        var sellRate = defaultSellRate(rules);
        var trade = bindings.tradeConfig(rules, bindings.boundNames(loc, worldId));
        boolean buyEnabled = PoiBindings.tradeAllows(trade, true);
        boolean sellEnabled = PoiBindings.tradeAllows(trade, false);

        var out = new ArrayList<MerchantInfo>();
        for (var npc : merchantsAt(loc)) {
            var meta = meta(npc);
            var offers = new ArrayList<Offer>();
            var wealthFactor = economyService.wealthFactor(loc.getWealth());
            var priceMod = meta.get("price_modifier") instanceof Number n ? n.doubleValue() : 1.0;
            var npcSellRate = meta.get("sell_rate") instanceof Number n ? n.doubleValue() : sellRate;
            for (var raw : assortment(meta)) {
                var name = text(raw.get("item"));
                if (name == null) continue;
                var item = findItem(items, name);
                int base = raw.get("price") instanceof Number n ? n.intValue()
                    : (item != null ? item.getValue() : 0);
                int price = raw.get("price") instanceof Number
                    ? base : (int) Math.round(base * wealthFactor * priceMod);
                int sellPrice = (int) Math.floor(price * npcSellRate);
                offers.add(new Offer(name, item != null ? item.getId().toString() : null,
                    price, sellPrice, item != null));
            }
            out.add(new MerchantInfo(npc.getId().toString(), npc.getName(),
                text(meta.get("occupation")), text(meta.get("greeting")), npcSellRate,
                buyEnabled, sellEnabled, offers));
        }
        return out;
    }

    @Transactional
    public TradeResult buy(UUID locationId, UUID npcId, UUID actorId, UUID userId,
                           String itemName, int qty, UUID campaignId) {
        requireQty(qty);
        var loc = location(locationId, userId);
        var worldId = worldIdOf(loc);
        var merchant = requireMerchant(npcId, loc, worldId);
        var resolvedCampaign = resolveCampaign(worldId, campaignId);
        var rules = rulesLoader.loadRules(resolvedCampaign, worldId);
        requireTrade(loc, worldId, rules, true);
        var items = itemsOf(rulesLoader.resolveSystem(resolvedCampaign, worldId));
        var offer = findOffer(loc, merchant, rules, items, itemName);
        var item = resolveItem(items, offer.item());
        var actor = lockedActor(actorId, userId, worldId);
        int total = totalPrice(offer.price(), qty);
        int before = currencyService.money(actor);
        currencyService.requirePayable(actor, total);
        currencyService.pay(actor, total);
        inventoryService.addItemInternal(actor, item.getId(), qty);
        entityRepo.save(actor);
        return result(merchant, offer, qty, total, before, currencyService.money(actor), rules);
    }

    @Transactional
    public TradeResult sell(UUID locationId, UUID npcId, UUID actorId, UUID userId,
                            String itemName, int qty, UUID campaignId) {
        requireQty(qty);
        var loc = location(locationId, userId);
        var worldId = worldIdOf(loc);
        var merchant = requireMerchant(npcId, loc, worldId);
        var resolvedCampaign = resolveCampaign(worldId, campaignId);
        var rules = rulesLoader.loadRules(resolvedCampaign, worldId);
        requireTrade(loc, worldId, rules, false);
        var items = itemsOf(rulesLoader.resolveSystem(resolvedCampaign, worldId));
        var offer = findOffer(loc, merchant, rules, items, itemName);
        var item = resolveItem(items, offer.item());
        var actor = lockedActor(actorId, userId, worldId);
        int have = inventoryQuantity(actor, item.getId());
        if (have < qty) {
            throw new MerchantException("ITEM_NOT_OWNED", "Has only " + have + "x " + item.getName());
        }
        int total = totalPrice(offer.sellPrice(), qty);
        int before = currencyService.money(actor);
        inventoryService.removeItemInternal(actor, item.getId(), qty);
        currencyService.credit(actor, total);
        entityRepo.save(actor);
        return result(merchant, offer, qty, total, before, currencyService.money(actor), rules);
    }

    // -------------------------------------------------------------- Helpers

    private static void requireQty(int qty) {
        if (qty < 1 || qty > MAX_QTY) {
            throw new MerchantException("TRADE_INVALID_QUANTITY",
                "Quantity must be between 1 and " + MAX_QTY);
        }
    }

    /** H-2: kein stiller Integer-Überlauf. */
    private static int totalPrice(int unitPrice, int qty) {
        try {
            return Math.multiplyExact(unitPrice, qty);
        } catch (ArithmeticException e) {
            throw new MerchantException("TRADE_INVALID_QUANTITY", "Quantity too large");
        }
    }

    /** H-3: Handel nur über die gebundene „Handeln"-Aktion und deren Flags. */
    private void requireTrade(Location loc, UUID worldId, Map<String, Object> rules, boolean buying) {
        var trade = bindings.tradeConfig(rules, bindings.boundNames(loc, worldId));
        if (trade == null) {
            throw new MerchantException("POI_ACTION_NOT_AVAILABLE", "No trade action bound here");
        }
        if (!PoiBindings.tradeAllows(trade, buying)) {
            throw new MerchantException("TRADE_DISABLED",
                buying ? "Buying is disabled here" : "Selling is disabled here");
        }
    }

    private List<GameItem> itemsOf(com.lwe.core.domain.GameSystem system) {
        return system == null ? List.of() : itemRepo.findByGameSystemId(system.getId());
    }

    private static GameItem findItem(List<GameItem> items, String name) {
        return items.stream().filter(i -> RuleNames.eq(i.getName(), name)).findFirst().orElse(null);
    }

    private TradeResult result(GameEntity merchant, Offer offer, int qty, int total,
                               int before, int after, Map<String, Object> rules) {
        return new TradeResult(merchant.getId().toString(), offer.item(), qty,
            qty > 0 ? total / qty : 0, total, before, after,
            currencyService.format(before, rules), currencyService.format(after, rules));
    }

    private Offer findOffer(Location loc, GameEntity merchant, Map<String, Object> rules,
                            List<GameItem> items, String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new MerchantException("ITEM_NOT_IN_ASSORTMENT", "Item name missing");
        }
        var meta = meta(merchant);
        var wealthFactor = economyService.wealthFactor(loc.getWealth());
        var priceMod = meta.get("price_modifier") instanceof Number n ? n.doubleValue() : 1.0;
        var npcSellRate = meta.get("sell_rate") instanceof Number n ? n.doubleValue() : defaultSellRate(rules);
        for (var raw : assortment(meta)) {
            var name = text(raw.get("item"));
            if (name == null || !RuleNames.eq(name, itemName)) continue;
            var item = findItem(items, name);
            if (item == null) {
                throw new MerchantException("MERCHANT_ITEM_UNKNOWN", "Unknown item: " + name);
            }
            int price = raw.get("price") instanceof Number n ? n.intValue()
                : (int) Math.round(item.getValue() * wealthFactor * priceMod);
            int sellPrice = (int) Math.floor(price * npcSellRate);
            return new Offer(name, item.getId().toString(), price, sellPrice, true);
        }
        throw new MerchantException("ITEM_NOT_IN_ASSORTMENT", "Not in assortment: " + itemName);
    }

    private GameItem resolveItem(List<GameItem> items, String name) {
        var item = findItem(items, name);
        if (item == null) {
            throw new MerchantException("MERCHANT_ITEM_UNKNOWN", "Unknown item: " + name);
        }
        return item;
    }

    private GameEntity requireMerchant(UUID npcId, Location loc, UUID worldId) {
        var npc = entityRepo.findById(npcId)
            .orElseThrow(() -> new MerchantException("MERCHANT_NOT_FOUND", "Merchant not found"));
        if (!worldId.equals(npc.getWorldId()) || !"NPC".equals(npc.getEntityType())) {
            throw new MerchantException("MERCHANT_NOT_FOUND", "Merchant not found");
        }
        var meta = meta(npc);
        if (!Boolean.TRUE.equals(meta.get("is_merchant"))) {
            throw new MerchantException("MERCHANT_NOT_FOUND", "NPC is not a merchant");
        }
        if (!loc.getId().toString().equals(String.valueOf(meta.get("location_id")))) {
            throw new MerchantException("MERCHANT_NOT_FOUND", "Merchant is not at this location");
        }
        return npc;
    }

    private List<GameEntity> merchantsAt(Location loc) {
        var worldId = worldIdOf(loc);
        var filter = "{\"location_id\":\"" + loc.getId() + "\"}";
        return entityRepo.findByWorldIdAndMetadataJsonFilter(worldId, filter).stream()
            .filter(e -> "NPC".equals(e.getEntityType()))
            .filter(e -> Boolean.TRUE.equals(meta(e).get("is_merchant")))
            .toList();
    }

    private Location location(UUID locationId, UUID userId) {
        var loc = locationRepo.findById(locationId)
            .orElseThrow(() -> new MerchantException("LOCATION_NOT_FOUND", "Location not found"));
        worldAccess.requireRead(worldIdOf(loc), userId);
        return loc;
    }

    private UUID worldIdOf(Location loc) {
        return regionRepo.findById(loc.getRegionId())
            .orElseThrow(() -> new MerchantException("REGION_NOT_FOUND", "Region not found")).getWorldId();
    }

    private UUID resolveCampaign(UUID worldId, UUID campaignId) {
        if (campaignId != null && rulesLoader.campaignBelongsToWorld(campaignId, worldId)) return campaignId;
        return campaignRepo.findByWorldId(worldId).stream().findFirst().map(c -> c.getId()).orElse(null);
    }

    private GameEntity lockedActor(UUID actorId, UUID userId, UUID worldId) {
        var actor = entityAccess.requireControl(actorId, userId);
        if (!actor.getWorldId().equals(worldId)) {
            throw new MerchantException("WORLD_ACCESS_DENIED", "Actor is not in this world");
        }
        return entityRepo.findByIdForUpdate(actor.getId())
            .orElseThrow(() -> new MerchantException("ENTITY_NOT_FOUND", "Actor not found"));
    }

    private double defaultSellRate(Map<String, Object> rules) {
        if (rules.get("poi_actions") instanceof List<?> actions) {
            for (var a : actions) {
                if (a instanceof Map<?, ?> m && m.get("trade") instanceof Map<?, ?> trade
                    && trade.get("sellRate") instanceof Number n) {
                    return n.doubleValue();
                }
            }
        }
        return 0.5;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> meta(GameEntity npc) {
        try {
            if (npc.getMetadataJson() == null || npc.getMetadataJson().isBlank()) return Map.of();
            return mapper.readValue(npc.getMetadataJson(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> assortment(Map<String, Object> meta) {
        if (!(meta.get("shop_inventory") instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(m -> (Map<String, Object>) m).toList();
    }

    private int inventoryQuantity(GameEntity owner, UUID itemId) {
        try {
            var raw = owner.getInventoryJson();
            if (raw == null || raw.isBlank()) return 0;
            var inv = mapper.readValue(raw,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            return inv.stream()
                .filter(e -> itemId.toString().equals(String.valueOf(e.get("itemId"))))
                .mapToInt(e -> e.get("quantity") instanceof Number n ? n.intValue() : 0)
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String text(Object value) {
        if (value == null) return null;
        var s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }

    public static class MerchantException extends RuntimeException {
        private final String errorCode;
        public MerchantException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
