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
class RestServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private GameSystemRepository gameSystemRepo;
    @Mock private WorldRepository worldRepo;

    private RestService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RestService(entityRepo, gameSystemRepo, worldRepo);
    }

    @Test
    void shouldFullHealWithDefaultConfig() {
        var entity = entityWithHp(5, 20, 0, 2);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rest(entity.getId());

        assertThat(entity.getHpCurrent()).isEqualTo(20);
        assertThat(entity.getApCurrent()).isEqualTo(2);
    }

    @Test
    void shouldApplyDiceHealing() {
        var entity = entityWithHp(5, 20, 0, 2);
        var gs = gsWithRestConfig("""
            "rest":{"hp_recovery":"dice","hp_dice":"1d6","ap_recovery":"full"}""");
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rest(entity.getId());

        assertThat(entity.getHpCurrent()).isBetween(6, 25); // 5 + (1 to 6)
        assertThat(entity.getApCurrent()).isEqualTo(2);
    }

    @Test
    void shouldApplyPercentageHealing() {
        var entity = entityWithHp(10, 50, 0, 2);
        var gs = gsWithRestConfig("""
            "rest":{"hp_recovery":"percentage","hp_percentage":50,"ap_recovery":"none"}""");
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(worldWithSystem()));
        when(gameSystemRepo.findById(gameSystemId)).thenReturn(Optional.of(gs));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rest(entity.getId());

        assertThat(entity.getHpCurrent()).isEqualTo(35); // 10 + 25 (50% of 50)
        assertThat(entity.getApCurrent()).isEqualTo(0); // unchanged
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

    private GameSystem gsWithRestConfig(String restJson) {
        var gs = new GameSystem("D20", 1, "{\"version\":1,\"attributes\":[]," + restJson + "}", "{}");
        setId(gs, gameSystemId);
        return gs;
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
