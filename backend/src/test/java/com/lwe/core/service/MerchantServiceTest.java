package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.Location;
import com.lwe.core.domain.Region;
import com.lwe.core.repository.*;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MerchantServiceTest {

    private final LocationRepository locationRepo = mock();
    private final RegionRepository regionRepo = mock();
    private final CampaignRepository campaignRepo = mock();
    private final GameEntityRepository entityRepo = mock();
    private final GameItemRepository itemRepo = mock();
    private final RulesLoader rulesLoader = mock();
    private final WorldAccess worldAccess = mock();
    private final EntityAccess entityAccess = mock();
    private final InventoryService inventoryService = mock();
    private final EconomyService economyService = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CurrencyService currencyService = new CurrencyService(mapper);

    private MerchantService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    private GameEntity actor;
    private GameItem potion;
    private GameItem sword;

    @BeforeEach
    void setUp() {
        service = new MerchantService(locationRepo, regionRepo, campaignRepo, entityRepo, itemRepo,
            rulesLoader, worldAccess, entityAccess, inventoryService, currencyService,
            economyService, new PoiBindings(entityRepo, mapper), mapper);

        var location = new Location(regionId, "village", "Bree");
        location.setServices("[\"Handeln\"]");
        setId(location, locationId);
        when(locationRepo.findById(locationId)).thenReturn(Optional.of(location));
        when(regionRepo.findById(regionId)).thenReturn(Optional.of(new Region(worldId, "Mittelreich")));
        when(campaignRepo.findByWorldId(worldId)).thenReturn(List.of());
        when(economyService.wealthFactor(anyInt())).thenReturn(1.0);

        var system = new GameSystem("Test", 1, "{}", null);
        when(rulesLoader.resolveSystem(any(), eq(worldId))).thenReturn(system);
        when(rulesLoader.loadRules(any(), eq(worldId))).thenReturn(Map.of(
            "currency", Map.of("denominations", List.of(
                Map.of("name", "Kupfer", "abbr", "K", "factor", 1),
                Map.of("name", "Gold", "abbr", "G", "factor", 100))),
            "poi_actions", List.of(Map.of("name", "Handeln",
                "trade", Map.of("buy", true, "sell", true, "sellRate", 0.5)))));

        var merchant = new GameEntity(worldId, "NPC", "Hugh");
        setId(merchant, merchantId);
        merchant.setMetadataJson("""
            {"is_merchant":true,"location_id":"%s","occupation":"Händler",
             "shop_inventory":[{"item":"Heiltrank"},{"item":"Langschwert","price":120},{"item":"Geisterklinge"}]}
            """.formatted(locationId));
        when(entityRepo.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(entityRepo.findByWorldIdAndMetadataJsonFilter(eq(worldId), anyString()))
            .thenReturn(List.of(merchant));

        potion = new GameItem(system.getId(), "Heiltrank", "CONSUMABLE", BigDecimal.ONE, 30);
        sword = new GameItem(system.getId(), "Langschwert", "WEAPON", BigDecimal.ONE, 100);
        setId(potion, UUID.randomUUID());
        setId(sword, UUID.randomUUID());
        when(itemRepo.findByGameSystemId(any())).thenReturn(List.of(potion, sword));

        actor = new GameEntity(worldId, "PC", "Mira");
        setId(actor, actorId);
        actor.setMetadataJson("{\"money\":100}");
        when(entityAccess.requireControl(actorId, userId)).thenReturn(actor);
        when(entityRepo.findByIdForUpdate(actorId)).thenReturn(Optional.of(actor));
    }

    /** JPA-IDs sind generiert und haben keinen Setter — für Unit-Tests per Reflection. */
    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listComputesPricesAndFlagsUnknownItems() {
        var merchants = service.list(locationId, userId, null);

        assertThat(merchants).hasSize(1);
        var offers = merchants.getFirst().offers();
        assertThat(offers).hasSize(3);
        var potionOffer = offers.stream().filter(o -> o.item().equals("Heiltrank")).findFirst().orElseThrow();
        assertThat(potionOffer.price()).isEqualTo(30);
        assertThat(potionOffer.sellPrice()).isEqualTo(15);
        var swordOffer = offers.stream().filter(o -> o.item().equals("Langschwert")).findFirst().orElseThrow();
        assertThat(swordOffer.price()).isEqualTo(120); // fester Sortimentspreis
        var ghost = offers.stream().filter(o -> o.item().equals("Geisterklinge")).findFirst().orElseThrow();
        assertThat(ghost.resolved()).isFalse();
    }

    @Test
    void buyMovesMoneyAndItem() {
        var result = service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 2, null);

        assertThat(currencyService.money(actor)).isEqualTo(40);
        assertThat(result.total()).isEqualTo(60);
        assertThat(result.moneyAfterText()).isEqualTo("40 K");
        verify(inventoryService).addItemInternal(actor, potion.getId(), 2);
    }

    @Test
    void buyWithoutFundsAppliesNothing() {
        actor.setMetadataJson("{\"money\":10}");

        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 2, null))
            .isInstanceOf(CurrencyService.CurrencyException.class);
        assertThat(currencyService.money(actor)).isEqualTo(10);
        verify(inventoryService, never()).addItemInternal(any(), any(), anyInt());
    }

    @Test
    void buyUnknownItemRejected() {
        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Geisterklinge", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("MERCHANT_ITEM_UNKNOWN");
    }

    @Test
    void buyItemNotInAssortmentRejected() {
        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Dolch", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("ITEM_NOT_IN_ASSORTMENT");
    }

    @Test
    void sellCreditsFloorPriceAndRemovesItem() {
        actor.setInventoryJson("[{\"itemId\":\"" + potion.getId() + "\",\"quantity\":3}]");

        var result = service.sell(locationId, merchantId, actorId, userId, "Heiltrank", 2, null);

        assertThat(currencyService.money(actor)).isEqualTo(130);
        assertThat(result.total()).isEqualTo(30);
        verify(inventoryService).removeItemInternal(actor, potion.getId(), 2);
    }

    @Test
    void sellWithoutOwnershipRejected() {
        actor.setInventoryJson("[]");
        assertThatThrownBy(() -> service.sell(locationId, merchantId, actorId, userId, "Heiltrank", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("ITEM_NOT_OWNED");
    }

    @Test
    void invalidQuantityRejected() {
        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 0, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("TRADE_INVALID_QUANTITY");
    }

    @Test
    void merchantNotAtLocationRejected() {
        var other = new GameEntity(worldId, "NPC", "Fremd");
        other.setMetadataJson("{\"is_merchant\":true,\"location_id\":\"" + UUID.randomUUID() + "\"}");
        when(entityRepo.findById(merchantId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("MERCHANT_NOT_FOUND");
    }

    @Test
    void buyRejectedWhenNoTradeActionBound() {
        locationRepo.findById(locationId).orElseThrow().setServices("[]");

        assertThatThrownBy(() -> service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("POI_ACTION_NOT_AVAILABLE");
    }

    @Test
    void sellRejectedWhenTradeDisallowsSelling() {
        when(rulesLoader.loadRules(any(), eq(worldId))).thenReturn(Map.of(
            "poi_actions", List.of(Map.of("name", "Handeln",
                "trade", Map.of("buy", true, "sell", false)))));
        actor.setInventoryJson("[{\"itemId\":\"" + potion.getId() + "\",\"quantity\":3}]");

        assertThatThrownBy(() -> service.sell(locationId, merchantId, actorId, userId, "Heiltrank", 1, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("TRADE_DISABLED");
    }

    @Test
    void hugeQuantityRejected() {
        assertThatThrownBy(() ->
            service.buy(locationId, merchantId, actorId, userId, "Heiltrank", 1_000_000, null))
            .isInstanceOf(MerchantService.MerchantException.class)
            .extracting(e -> ((MerchantService.MerchantException) e).getErrorCode())
            .isEqualTo("TRADE_INVALID_QUANTITY");
        verify(inventoryService, never()).addItemInternal(any(), any(), anyInt());
    }

    @Test
    void listReportsTradeFlags() {
        var merchants = service.list(locationId, userId, null);
        assertThat(merchants.getFirst().buyEnabled()).isTrue();
        assertThat(merchants.getFirst().sellEnabled()).isTrue();
    }
}
