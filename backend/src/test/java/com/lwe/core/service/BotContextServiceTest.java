package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.World;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotContextServiceTest {

    @Mock private WorldRepository worldRepo;
    @Mock private CampaignRepository campaignRepo;

    private BotContextService service;
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BotContextService(worldRepo, campaignRepo, new ObjectMapper());
    }

    @Test
    void includesWorldModeAndCampaignModes() {
        var world = new World("W", UUID.randomUUID(), "{\"ai_mode\":\"suggest\"}");
        try {
            var f = World.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(world, worldId);
        } catch (Exception e) { throw new RuntimeException(e); }
        var campaign = new Campaign(worldId, UUID.randomUUID(), "Runde");
        campaign.setSettingsJson("{\"bot\":{\"mode\":\"off\"}}");

        when(worldRepo.findByActiveTrue()).thenReturn(List.of(world));
        when(campaignRepo.findByWorldId(worldId)).thenReturn(List.of(campaign));

        var contexts = service.listBotContexts();

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().worldAiMode()).isEqualTo("suggest");
        assertThat(contexts.getFirst().campaigns()).hasSize(1);
        assertThat(contexts.getFirst().campaigns().getFirst().botMode()).isEqualTo("off");
    }
}
