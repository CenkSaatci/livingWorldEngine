package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.repository.GameSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private GameItemRepository itemRepo;
    @Mock private GameSystemRepository systemRepo;
    @Mock private GameEntityRepository entityRepo;

    private ItemService service;
    private final UUID gameSystemId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ItemService(itemRepo, systemRepo, entityRepo, new ObjectMapper());
    }

    private void stubSystemExists() {
        var system = new GameSystem("DSA", 1, "{}", "{}");
        setId(system, gameSystemId);
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));
    }

    @Test
    void shouldCreateItem() {
        stubSystemExists();
        when(itemRepo.save(any())).thenAnswer(inv -> {
            var i = inv.<GameItem>getArgument(0);
            setId(i, UUID.randomUUID());
            return i;
        });

        var item = service.create(gameSystemId, userId, "Kurzschwert", "WEAPON",
            new BigDecimal("1.50"), 10, "{\"damage\":\"1d6\"}", "{}");

        assertThat(item.getName()).isEqualTo("Kurzschwert");
        assertThat(item.getGameSystemId()).isEqualTo(gameSystemId);
        assertThat(item.getBonusesJson()).isEqualTo("{\"damage\":\"1d6\"}");
        verify(itemRepo).save(any());
    }

    @Test
    void shouldRejectUnknownSystem() {
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(gameSystemId, userId, "X", "WEAPON",
            BigDecimal.ONE, 1, "{}", "{}"))
            .isInstanceOf(ItemService.ItemException.class)
            .matches(e -> ((ItemService.ItemException) e).getErrorCode().equals("GAME_SYSTEM_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidType() {
        stubSystemExists();

        assertThatThrownBy(() -> service.create(gameSystemId, userId, "X", "WAND",
            BigDecimal.ONE, 1, "{}", "{}"))
            .isInstanceOf(ItemService.ItemException.class)
            .matches(e -> ((ItemService.ItemException) e).getErrorCode().equals("INVALID_ITEM_TYPE"));
    }

    @Test
    void shouldListByGameSystem() {
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(new GameSystem("DSA", 1, "{}", "{}")));
        when(itemRepo.findByGameSystemId(gameSystemId)).thenReturn(java.util.List.of());

        var items = service.listByGameSystem(gameSystemId);

        assertThat(items).isEmpty();
        verify(itemRepo).findByGameSystemId(gameSystemId);
    }

    @Test
    void shouldUpdateItem() {
        var item = new GameItem(gameSystemId, "Kurzschwert", "WEAPON", BigDecimal.ONE, 10);
        setId(item, UUID.randomUUID());
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(item.getId(), "Langschwert", null, null, null, null, null);

        assertThat(updated.getName()).isEqualTo("Langschwert");
        assertThat(updated.getValue()).isEqualTo(10);
    }

    @Test
    void shouldDeleteItem() {
        var item = new GameItem(gameSystemId, "Kurzschwert", "WEAPON", BigDecimal.ONE, 10);
        setId(item, UUID.randomUUID());
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));

        service.delete(item.getId());

        verify(itemRepo).delete(item);
    }

    @Test
    void deleteRemovesItemFromAllInventories() throws Exception {
        var itemId = UUID.randomUUID();
        var item = new GameItem(gameSystemId, "Kurzschwert", "WEAPON", BigDecimal.ONE, 10);
        setId(item, itemId);
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));

        var worldId = UUID.randomUUID();
        var entity = new GameEntity(worldId, "PC", "Aragorn");
        setId(entity, UUID.randomUUID());
        var mapper = new ObjectMapper();
        entity.setInventoryJson(mapper.writeValueAsString(java.util.List.of(
            java.util.Map.of("itemId", itemId.toString(), "quantity", 1, "equipped", true, "slot", "hand"))));
        when(entityRepo.findAll()).thenReturn(java.util.List.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(itemId);

        var inv = mapper.readTree(entity.getInventoryJson());
        assertThat(inv.size()).isEqualTo(0);
        verify(entityRepo).save(entity);
        verify(itemRepo).delete(item);
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
