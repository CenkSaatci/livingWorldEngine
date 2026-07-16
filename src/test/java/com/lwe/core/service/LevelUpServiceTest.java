package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LevelUpServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private GameSystemRepository gameSystemRepo;
    @Mock private WorldRepository worldRepo;

    private LevelUpService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();
    private GameSystem gs;

    @BeforeEach
    void setUp() {
        service = new LevelUpService(entityRepo, gameSystemRepo, worldRepo);
        gs = new GameSystem("D20", 1, LEVELS_JSON, "{}");
        setId(gs, gameSystemId);
    }

    @Test
    void shouldAddXpAndLevelUp() {
        var entity = entityWithXp(0);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addXp(entity.getId(), 250);

        assertThat(entity.getExperiencePoints()).isEqualTo(250);
        assertThat(entity.getUnspentAttributePoints()).isEqualTo(3);
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

    private World worldWithSystem() {
        var w = new World("Test", UUID.randomUUID(), gameSystemId, "{}");
        setId(w, worldId);
        return w;
    }

    private GameEntity entityWithXp(int xp) {
        var e = new GameEntity(worldId, "PC", "Aragorn");
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
