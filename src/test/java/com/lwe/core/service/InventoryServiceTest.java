package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    private final GameEntityRepository entityRepo = mock();
    private final GameItemRepository itemRepo = mock();
    private final com.lwe.core.util.WorldAccess worldAccess = mock();
    private InventoryService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID entityId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InventoryService(entityRepo, itemRepo, worldAccess, new ObjectMapper());
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
    }

    private GameEntity entityWithInventory(String invJson) {
        var e = new GameEntity(worldId, "PC", "Hero");
        e.setInventoryJson(invJson);
        try { var f = GameEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(e, entityId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        return e;
    }

    private GameItem createItem(String type, String bonuses) {
        var item = new GameItem(worldId, "Short Sword", type, BigDecimal.ONE, 10);
        item.setBonusesJson(bonuses);
        try { var f = GameItem.class.getDeclaredField("id"); f.setAccessible(true); f.set(item, itemId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        return item;
    }

    @Test
    void shouldGetEmptyInventory() {
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entityWithInventory("[]")));

        var result = service.getInventory(entityId, userId);
        assertThat(result.items()).isEmpty();
        assertThat(result.computedBonuses()).isEmpty();
    }

    @Test
    void shouldAddItemToInventory() {
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entityWithInventory("[]")));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(createItem("WEAPON", "{}")));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addItem(entityId, userId, itemId, 1);
        verify(entityRepo).save(any());
    }

    @Test
    void shouldStackDuplicateItems() {
        var entity = entityWithInventory("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":false,\"slot\":null}]");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(createItem("WEAPON", "{}")));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addItem(entityId, userId, itemId, 2);
        assertThat(entity.getInventoryJson()).contains("\"quantity\":3");
    }

    @Test
    void shouldEquipItemAndComputeBonuses() {
        var entity = entityWithInventory("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":false,\"slot\":null}]");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(createItem("WEAPON", "{\"staerke\":2,\"initiative\":1}")));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.equipItem(entityId, userId, itemId, "weapon");

        assertThat(result.computedBonuses()).containsEntry("staerke", 2);
        assertThat(result.computedBonuses()).containsEntry("initiative", 1);
        assertThat(result.items().getFirst().equipped()).isTrue();
    }

    @Test
    void shouldUnequipItem() {
        var entity = entityWithInventory("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":true,\"slot\":\"weapon\"}]");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(createItem("WEAPON", "{}")));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.unequipItem(entityId, userId, itemId);
        assertThat(result.items().getFirst().equipped()).isFalse();
    }

    @Test
    void shouldRejectRemovingMoreThanAvailable() {
        var entity = entityWithInventory("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":false,\"slot\":null}]");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.removeItem(entityId, userId, itemId, 5))
            .isInstanceOf(InventoryService.InventoryException.class);
    }

    @Test
    void shouldAutoUnequipWhenRemovingEquippedItem() {
        var entity = entityWithInventory("[{\"itemId\":\"" + itemId + "\",\"quantity\":1,\"equipped\":true,\"slot\":\"weapon\"}]");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.removeItem(entityId, userId, itemId, 1);
        verify(entityRepo).save(any());
    }
}