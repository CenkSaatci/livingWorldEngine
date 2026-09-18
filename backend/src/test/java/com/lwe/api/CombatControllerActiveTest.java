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
        when(combatService.getParticipants(eq(session.getId()), eq(userId))).thenReturn(List.of());

        var res = controller.getActiveSession(worldId, user());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().session().id()).isEqualTo(session.getId());
    }

    @Test
    void maneuverResponseCarriesRollBreakdown() {
        var session = new CombatSession(worldId, null);
        var sessionId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        setId(session, sessionId);
        var damage = new CombatService.RollBreakdown("damage", List.of(4),
            List.of(new CombatService.RollPart("Bonus", 1)), null, 5, null, null, null);
        var action = new CombatService.CombatActionResult("MANEUVER:Wuchtschlag", 5, 1, true, null,
            null, damage, 0);
        when(combatService.executeManeuver(userId, sessionId, actorId, targetId, "Wuchtschlag"))
            .thenReturn(action);
        when(combatService.getSession(userId, sessionId)).thenReturn(session);
        when(combatService.getParticipants(sessionId, userId)).thenReturn(List.of());

        var res = controller.maneuver(sessionId,
            new CombatController.ManeuverRequest(actorId, targetId, "Wuchtschlag"), user());

        // R2-Fix: Manöver liefern die Aufstellung mit (vorher fiel sie im Controller weg).
        assertThat(res.getBody().result()).isNotNull();
        assertThat(res.getBody().result().damage().total()).isEqualTo(5);
    }

    @Test
    void abilityResponseCarriesRollBreakdown() {
        var session = new CombatSession(worldId, null);
        var sessionId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        var abilityId = UUID.randomUUID();
        setId(session, sessionId);
        var damage = new CombatService.RollBreakdown("damage", List.of(6), List.of(),
            null, 6, null, null, null);
        var action = new CombatService.CombatActionResult("ABILITY:Feuerball", 6, 0, true, null,
            null, damage, 0);
        when(combatService.useAbility(userId, sessionId, actorId, abilityId, null))
            .thenReturn(action);
        when(combatService.getSession(userId, sessionId)).thenReturn(session);
        when(combatService.getParticipants(sessionId, userId)).thenReturn(List.of());

        var res = controller.useAbility(sessionId,
            new CombatController.AbilityRequest(actorId, abilityId, null), user());

        assertThat(res.getBody().result()).isNotNull();
        assertThat(res.getBody().result().damage().dice()).containsExactly(6);
    }

    private void setId(CombatSession session, UUID id) {
        try {
            var f = CombatSession.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(session, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
