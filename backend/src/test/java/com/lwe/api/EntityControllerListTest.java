package com.lwe.api;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.User;
import com.lwe.core.service.EntityService;
import com.lwe.core.service.RestService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EntityControllerListTest {

    private final EntityService entityService = mock();
    private final RestService restService = mock();
    private final EntityController controller = new EntityController(entityService, restService);

    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private User user() {
        var u = new User("t@t.com", "t", "hash", "USER", "de");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, userId);
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    @Test
    void forTradeReturnsSlimCandidatesWithoutSheetData() {
        var e = new GameEntity(worldId, "PC", "Fremd");
        e.setAttributesJson("{\"mut\":99}");
        e.setInventoryJson("[{\"itemId\":\"secret\"}]");
        e.setMetadataJson("{\"traits\":[\"Geheim\"]}");
        e.setBackstory("Secret");
        when(entityService.list(worldId, userId, null, true, null)).thenReturn(List.of(e));

        var res = controller.list(worldId, null, true, null, user());

        // R2-Fix: Handelskandidaten nur als Referenz — keine Sheet-/Inventar-Daten.
        var dto = (EntityController.TradeCandidateResponse) ((List<?>) res.getBody()).getFirst();
        assertThat(dto.name()).isEqualTo("Fremd");
        assertThat(dto.entityType()).isEqualTo("PC");
    }

    @Test
    void normalListStillReturnsFullEntity() {
        var e = new GameEntity(worldId, "PC", "Eigen");
        e.setAttributesJson("{\"mut\":12}");
        when(entityService.list(worldId, userId, null, false, null)).thenReturn(List.of(e));

        var res = controller.list(worldId, null, false, null, user());

        var dto = (com.lwe.api.dto.EntityResponse) ((List<?>) res.getBody()).getFirst();
        assertThat(dto.attributesJson()).contains("mut");
    }
}
