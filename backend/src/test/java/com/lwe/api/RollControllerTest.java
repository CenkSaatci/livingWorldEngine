package com.lwe.api;

import com.lwe.api.dto.ProbeResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.ProbeService;
import com.lwe.core.service.RollService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RollControllerTest {

    private final RollService rollService = mock();
    private final ProbeService probeService = mock();
    private final RollController controller = new RollController(rollService, probeService);

    private final UUID userId = UUID.randomUUID();
    private final UUID entityId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    private User user() {
        var u = new User("t@t.com", "t", "hash", "USER", "de");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, userId);
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    private ProbeResponse response() {
        return new ProbeResponse("d20_target", new int[]{10}, 0, 10, true, List.of(), List.of());
    }

    @Test
    void probePassesDifficultyToService() {
        when(probeService.executeProbe(eq(entityId), eq(userId), eq("Klettern"), eq(10), eq(false),
            eq(campaignId), eq(2))).thenReturn(response());

        var res = controller.probe(
            new RollController.ProbeRequest(entityId, "Klettern", 10, false, campaignId, 2), user());

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        verify(probeService).executeProbe(entityId, userId, "Klettern", 10, false, campaignId, 2);
    }

    @Test
    void probeDefaultsDifficultyToZero() {
        when(probeService.executeProbe(any(), any(), any(), anyInt(), anyBoolean(), any(), eq(0)))
            .thenReturn(response());

        controller.probe(new RollController.ProbeRequest(entityId, "Klettern", 10, false, campaignId, null), user());

        verify(probeService).executeProbe(entityId, userId, "Klettern", 10, false, campaignId, 0);
    }

    @Test
    void castPassesTargetAndDifficulty() {
        when(probeService.cast(entityId, userId, "Odem", campaignId, 100, 2))
            .thenReturn(new ProbeService.CastResult(response(), "asp", 2, 18, 20));

        var res = controller.cast(
            new RollController.CastRequest(entityId, "Odem", campaignId, 100, 2), user());

        assertThat(res.getBody().resourceRemaining()).isEqualTo(18);
        verify(probeService).cast(entityId, userId, "Odem", campaignId, 100, 2);
    }

    @Test
    void castPassesNullsForDefaults() {
        when(probeService.cast(entityId, userId, "Odem", campaignId, null, null))
            .thenReturn(new ProbeService.CastResult(response(), "asp", 2, 18, 20));

        controller.cast(new RollController.CastRequest(entityId, "Odem", campaignId, null, null), user());

        verify(probeService).cast(entityId, userId, "Odem", campaignId, null, null);
    }
}
