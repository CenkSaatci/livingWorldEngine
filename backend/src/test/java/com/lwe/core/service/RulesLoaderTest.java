package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RulesLoaderTest {

    @Mock private WorldRepository worldRepo;
    @Mock private GameSystemRepository systemRepo;
    @Mock private CampaignRepository campaignRepo;

    private RulesLoader loader;
    private final UUID campaignId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loader = new RulesLoader(worldRepo, systemRepo, campaignRepo, new ObjectMapper());
    }

    @Test
    void loadRulesByCampaignReturnsSystemRules() {
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        var system = new GameSystem("DSA", 1,
            "{\"attributes\":[{\"key\":\"mut\",\"name\":\"Mut\"}]}", "{}");
        setId(system, gameSystemId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));

        var rules = loader.loadRulesByCampaign(campaignId);

        assertThat(rules).containsKey("attributes");
    }

    @Test
    void pinnedSnapshotWinsOverLiveSystem() {
        // P27-T05: Kampagne bleibt auf ihrer Version, auch wenn das System geaendert wurde.
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        campaign.setRulesJsonSnapshot("{\"marker\":\"alt\"}");
        campaign.setGameSystemVersion(1);
        var system = new GameSystem("DSA", 2, "{\"marker\":\"neu\"}", "{}");
        setId(system, gameSystemId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));

        var rules = loader.loadRulesByCampaign(campaignId);

        assertThat(rules).containsEntry("marker", "alt");
    }

    @Test
    void loadSystemByCampaignReturnsSnapshotView() {
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        campaign.setRulesJsonSnapshot("{\"marker\":\"pin\"}");
        campaign.setGameSystemVersion(2);
        var live = new GameSystem("DSA", 5, "{\"marker\":\"live\"}", "{}");
        setId(live, gameSystemId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(live));

        var gs = loader.loadSystemByCampaign(campaignId);

        assertThat(gs.getRulesJson()).contains("pin");
        assertThat(gs.getVersion()).isEqualTo(2);
        assertThat(gs.getName()).isEqualTo("DSA");
    }

    @Test
    void loadRulesByCampaignReturnsEmptyForUnknownCampaign() {
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.empty());

        var rules = loader.loadRulesByCampaign(campaignId);

        assertThat(rules).isEmpty();
    }

    @Test
    void loadRulesByCampaignReturnsEmptyForUnknownSystem() {
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.empty());

        var rules = loader.loadRulesByCampaign(campaignId);

        assertThat(rules).isEmpty();
    }

    @Test
    void loadSystemByCampaignReturnsSystem() {
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        var system = new GameSystem("DSA", 1, "{}", "{}");
        setId(system, gameSystemId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));

        var result = loader.loadSystemByCampaign(campaignId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(gameSystemId);
    }

    @Test
    void loadSystemByCampaignReturnsNullForUnknownCampaign() {
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.empty());

        assertThat(loader.loadSystemByCampaign(campaignId)).isNull();
    }

    @Test
    void loadRulesByCampaignPrefersCampaignOverWorldFallback() {
        var campaign = new Campaign(UUID.randomUUID(), gameSystemId, "Runde 1");
        setId(campaign, campaignId);
        var system = new GameSystem("DSA", 1, "{\"campaign_rules\":true}", "{}");
        setId(system, gameSystemId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));

        var rules = loader.loadRules(campaignId, UUID.randomUUID());

        assertThat(rules).containsKey("campaign_rules");
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
