package com.lwe.rules;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.service.RulesLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EngineResolverTest {

    private final RulesLoader rulesLoader = mock();
    private final D20RuleEngine d20 = new D20RuleEngine();
    private final EngineResolver resolver = new EngineResolver(rulesLoader, List.of(d20));

    private final UUID campaignId = UUID.randomUUID();
    private final World world = new World("W", UUID.randomUUID(), "{}");

    @Test
    void resolvesEngineOfCampaignSystem() {
        when(rulesLoader.resolveSystem(any(UUID.class), any(World.class)))
            .thenReturn(new GameSystem("D20", 1, "{\"dice_mechanics\":{\"probe\":\"1d20+mod\"}}", "{}"));

        assertThat(resolver.resolve(world, campaignId)).isSameAs(d20);
    }

    @Test
    void fallsBackToD20WithoutSystem() {
        when(rulesLoader.resolveSystem(any(UUID.class), any(World.class))).thenReturn(null);

        assertThat(resolver.resolve(world, campaignId)).isSameAs(d20);
    }

    @Test
    void fallsBackToD20OnInvalidProbe() {
        when(rulesLoader.resolveSystem(any(UUID.class), any(World.class)))
            .thenReturn(new GameSystem("Kaputt", 1, "{\"dice_mechanics\":{\"probe\":\"wuerfel\"}}", "{}"));

        assertThat(resolver.resolve(world, campaignId)).isSameAs(d20);
    }
}
