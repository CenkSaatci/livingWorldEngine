package com.lwe.api;

import com.lwe.core.domain.CombatSession;
import com.lwe.core.domain.User;
import com.lwe.core.service.CombatService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CombatControllerActiveTest {

    private final CombatService combatService = mock();
    private final CombatController controller = new CombatController(combatService);

    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

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
    void activeSessionIsNoContentWhenNone() {
        when(combatService.findActiveSession(userId, worldId)).thenReturn(Optional.empty());

        var res = controller.getActiveSession(worldId, user());

        assertThat(res.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void activeSessionReturnsSessionWithParticipants() {
        var session = new CombatSession(worldId, null);
        try {
            var f = CombatSession.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(session, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
        when(combatService.findActiveSession(userId, worldId)).thenReturn(Optional.of(session));
        when(combatService.getParticipants(session.getId())).thenReturn(List.of());

        var res = controller.getActiveSession(worldId, user());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().session().id()).isEqualTo(session.getId());
    }
}
