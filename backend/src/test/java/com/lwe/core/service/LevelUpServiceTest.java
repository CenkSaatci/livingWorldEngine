package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LevelUpServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private RulesLoader rulesLoader;

    private LevelUpService service;
    private GameSystem gs;

    @BeforeEach
    void setUp() {
        service = new LevelUpService(entityRepo, rulesLoader, new ObjectMapper());
        gs = new GameSystem("D20", 1, LEVELS_JSON, "{}");
        setId(gs, UUID.randomUUID());
    }

    @Test
    void shouldAddXpAndLevelUp() {
        var entity = entityWithXp(0);
        var campaignId = UUID.randomUUID();
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(rulesLoader.loadSystemByCampaign(campaignId)).thenReturn(gs);
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addXp(entity.getId(), 250, campaignId);

        assertThat(entity.getExperiencePoints()).isEqualTo(250);
        assertThat(entity.getUnspentAttributePoints()).isEqualTo(3);
    }

    @Test
    void shouldCreditXpWithoutSystem() {
        var entity = entityWithXp(0);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(rulesLoader.loadSystemByCampaign(null)).thenReturn(null);
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addXp(entity.getId(), 250);

        assertThat(entity.getExperiencePoints()).isEqualTo(250);
        assertThat(entity.getUnspentAttributePoints()).isEqualTo(0);
    }

    @Test
    void shouldGetLevelOneForZeroXp() {
        var entity = entityWithXp(0);
        int level = service.getLevel(entity, gs);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void shouldDistributeAttributePoints() {
        var entity = entityWithXp(0);
        entity.setUnspentAttributePoints(5);
        entity.setAttributesJson("{\"staerke\":10,\"geschick\":10}");
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.spendPoints(entity.getId(), "staerke", 3);

        assertThat(entity.getUnspentAttributePoints()).isEqualTo(2);
        assertThat(entity.getAttributesJson()).contains("\"staerke\":13");
    }

    @Test
    void shouldResetPoints() {
        var entity = entityWithXp(0);
        entity.setAttributesJson("{\"staerke\":15,\"geschick\":12,\"__spent__\":{\"staerke\":3,\"geschick\":2}}");
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPoints(entity.getId());

        assertThat(entity.getUnspentAttributePoints()).isEqualTo(5);
        assertThat(entity.getAttributesJson()).doesNotContain("__spent__");
        assertThat(entity.getAttributesJson()).contains("\"staerke\":12");
    }

    private GameEntity entityWithXp(int xp) {
        var e = new GameEntity(UUID.randomUUID(), "PC", "Aragorn");
        setId(e, UUID.randomUUID());
        e.setExperiencePoints(xp);
        return e;
    }

    private static final String LEVELS_JSON = """
        {"version":1,"attributes":[{"name":"staerke","type":"INT","default":10}],
        "progression":{"mode":"level","levels":[
            {"level":1,"xp":0,"attribute_points":0,"ability_slots":0},
            {"level":2,"xp":100,"attribute_points":3,"ability_slots":1},
            {"level":3,"xp":300,"attribute_points":3,"ability_slots":1}
        ]}}
        """;

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
