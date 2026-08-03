package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import com.lwe.core.util.WorldAccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private GameSystemRepository gameSystemRepo;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldAccess worldAccess;

    private RestService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RestService(entityRepo, gameSystemRepo, worldRepo, worldAccess, new ObjectMapper());
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
    }

    private static final String NEW_REST_CONFIG = """
        "dice_mechanics":{"probe":"1d20","combat":{
          "initiative":"1d20","damage":"1d8",
          "resting":{
            "short_rest":{"hp":"50%","ap":"full","recover":["resources"]},
            "long_rest":{"hp":"full","ap":"full","recover":["all"]}
          }
        }}
        """;

    @Test
    void shortRestHealsPercentageHp() {
        var entity = entityWithHp(10, 50, 0, 2);
        var gs = gsWithConfig(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(35); // 10 + 50% of 50 = 35
    }

    @Test
    void shortRestRestoresAp() {
        var entity = entityWithHp(50, 50, 0, 2);
        var gs = gsWithConfig(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getApCurrent()).isEqualTo(2); // restored to max
    }

    @Test
    void longRestFullyHeals() {
        var entity = entityWithHp(10, 50, 0, 2);
        var gs = gsWithConfig(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.longRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(50);
        assertThat(entity.getApCurrent()).isEqualTo(2);
    }

    @Test
    void shortRestWithDiceExpression() {
        var entity = entityWithHp(10, 50, 0, 2);
        var gs = gsWithConfig("""
            "dice_mechanics":{"probe":"1d20","combat":{
              "initiative":"1d20","damage":"1d8",
              "resting":{"short_rest":{"hp":"1d6","ap":null,"recover":[]}}
            }}
            """);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isBetween(11, 16); // 10 + (1 to 6)
    }

    @Test
    void restWithoutConfigDefaultsToNoop() {
        var entity = entityWithHp(5, 20, 0, 2);
        var gs = gsWithConfig("""
            {"version":1,"attributes":[],"dice_mechanics":{"probe":"1d20"}}
            """);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(5); // unchanged
    }

    private GameEntity entityWithHp(int hp, int hpMax, int ap, int apMax) {
        var e = new GameEntity(worldId, "PC", "Test");
        setId(e, UUID.randomUUID());
        e.setHpCurrent(hp);
        e.setHpMax(hpMax);
        e.setApCurrent(ap);
        e.setApMax(apMax);
        return e;
    }

    private World worldWithSystem() {
        var w = new World("Test", UUID.randomUUID(), gameSystemId, "{}");
        setId(w, worldId);
        return w;
    }

    private GameSystem gsWithConfig(String rulesJson) {
        var gs = new GameSystem("D20", 1, rulesJson.startsWith("{") ? rulesJson : "{\"version\":1,\"attributes\":[]," + rulesJson + "}", "{}");

        setId(gs, gameSystemId);
        return gs;
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
