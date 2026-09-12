package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.Trade;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.repository.TradeRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock private TradeRepository tradeRepo;
    @Mock private GameEntityRepository entityRepo;
    @Mock private GameItemRepository itemRepo;
    @Mock private InventoryService inventoryService;
    @Mock private WorldAccess worldAccess;

    private TradeService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(entityRepo.findByIdForUpdate(any())).thenAnswer(inv ->
            entityRepo.findById(inv.getArgument(0)));
        service = new TradeService(tradeRepo, entityRepo, itemRepo, inventoryService,
            worldAccess, new com.lwe.core.util.EntityAccess(entityRepo, worldAccess), new ObjectMapper());
    }

    private GameEntity entity(UUID id, String inventoryJson) {
        var e = new GameEntity(worldId, "PC", "Held-" + id.toString().substring(0, 4));
        e.setAttributesJson("{}");
        e.setInventoryJson(inventoryJson);
        try {
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, id);
        } catch (Exception ex) { throw new RuntimeException(ex); }
        return e;
    }

    private void stubEntities(GameEntity a, GameEntity b) {
        lenient().when(entityRepo.findById(a.getId())).thenReturn(Optional.of(a));
        lenient().when(entityRepo.findById(b.getId())).thenReturn(Optional.of(b));
    }

    @Test
    void proposeAndAcceptSwapsAtomically() {
        var proposerId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var proposer = entity(proposerId, "{\"items\":[]}");
        proposer.setInventoryJson("[{\"itemId\":\"" + itemId + "\",\"quantity\":3}]");
        var partner = entity(partnerId, "[]");
        stubEntities(proposer, partner);
        var item = mock(GameItem.class);
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));
        when(tradeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var trade = service.propose(worldId, userId, proposerId, partnerId,
            List.of(Map.of("itemId", itemId.toString(), "quantity", 2)), List.of());
        assertThat(trade.getStatus()).isEqualTo("proposed");

        setId(trade, UUID.randomUUID());
        when(tradeRepo.findById(trade.getId())).thenReturn(Optional.of(trade));
        var done = service.accept(trade.getId(), userId, partnerId);

        assertThat(done.getStatus()).isEqualTo("accepted");
        verify(inventoryService).removeItem(proposerId, userId, itemId, 2);
        verify(inventoryService).addItem(partnerId, userId, itemId, 2);
        // Runde 1 (F6): Trade-Zeile und Entities werden gesperrt geladen.
        verify(tradeRepo).findByIdForUpdate(trade.getId());
        verify(entityRepo, org.mockito.Mockito.atLeastOnce()).findByIdForUpdate(any());
    }

    @Test
    void counterOfferAcceptMovesFromLastEditor() {
        var aId = UUID.randomUUID();
        var bId = UUID.randomUUID();
        var swordId = UUID.randomUUID();
        var potionId = UUID.randomUUID();
        var a = entity(aId, "[{\"itemId\":\"" + swordId + "\",\"quantity\":1}]");
        var b = entity(bId, "[{\"itemId\":\"" + potionId + "\",\"quantity\":2}]");
        stubEntities(a, b);
        when(itemRepo.findById(any())).thenReturn(Optional.of(mock(GameItem.class)));
        when(tradeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var trade = service.propose(worldId, userId, aId, bId,
            List.of(Map.of("itemId", swordId.toString(), "quantity", 1)), List.of());
        setId(trade, UUID.randomUUID());
        when(tradeRepo.findById(trade.getId())).thenReturn(Optional.of(trade));

        // B kontert: gibt Trank, will Schwert
        service.counter(trade.getId(), userId, bId,
            List.of(Map.of("itemId", potionId.toString(), "quantity", 1)),
            List.of(Map.of("itemId", swordId.toString(), "quantity", 1)));
        service.accept(trade.getId(), userId, aId);

        verify(inventoryService).removeItem(bId, userId, potionId, 1);
        verify(inventoryService).addItem(aId, userId, potionId, 1);
        verify(inventoryService).removeItem(aId, userId, swordId, 1);
        verify(inventoryService).addItem(bId, userId, swordId, 1);
    }

    @Test
    void foreignOwnedEntityNeedsDm() {
        var aId = UUID.randomUUID();
        var bId = UUID.randomUUID();
        var a = entity(aId, "[]");
        a.setOwnerUserId(UUID.randomUUID()); // gehoert jemand anderem
        stubEntities(a, entity(bId, "[]"));
        doThrow(new com.lwe.core.util.WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "denied"))
            .when(worldAccess).requireDm(worldId, userId);

        assertThatThrownBy(() -> service.propose(worldId, userId, aId, bId, List.of(), List.of()))
            .isInstanceOf(com.lwe.core.util.WorldAccess.WorldAccessException.class);
    }

    @Test
    void counterSwitchesLastEditor() {
        var proposerId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var proposer = entity(proposerId, "[]");
        var partner = entity(partnerId, "[]");
        stubEntities(proposer, partner);
        var trade = new Trade(worldId, proposerId, partnerId, "[]", "[]");
        setId(trade, UUID.randomUUID());
        when(tradeRepo.findById(trade.getId())).thenReturn(Optional.of(trade));
        when(tradeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.counter(trade.getId(), userId, partnerId, List.of(), List.of());

        assertThat(result.getLastEditorEntityId()).isEqualTo(partnerId);
    }

    @Test
    void selfAcceptRejected() {
        var proposerId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        stubEntities(entity(proposerId, "[]"), entity(partnerId, "[]"));
        var trade = new Trade(worldId, proposerId, partnerId, "[]", "[]");
        setId(trade, UUID.randomUUID());
        when(tradeRepo.findById(trade.getId())).thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> service.accept(trade.getId(), userId, proposerId))
            .isInstanceOf(TradeService.TradeException.class)
            .matches(e -> ((TradeService.TradeException) e).getErrorCode().equals("TRADE_SELF_ACCEPT"));
    }

    @Test
    void proposeWithoutStockRejected() {
        var proposerId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        stubEntities(entity(proposerId, "[]"), entity(partnerId, "[]"));
        var item = mock(GameItem.class);
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.propose(worldId, userId, proposerId, partnerId,
                List.of(Map.of("itemId", itemId.toString(), "quantity", 5)), List.of()))
            .isInstanceOf(TradeService.TradeException.class)
            .matches(e -> ((TradeService.TradeException) e).getErrorCode().equals("TRADE_INSUFFICIENT_QUANTITY"));
    }

    @org.junit.jupiter.api.BeforeEach
    void stubTradeLock() {
        lenient().when(tradeRepo.findByIdForUpdate(any())).thenAnswer(inv ->
            tradeRepo.findById(inv.getArgument(0)));
    }

    private void setId(Trade trade, UUID id) {
        try {
            var f = Trade.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(trade, id);
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
