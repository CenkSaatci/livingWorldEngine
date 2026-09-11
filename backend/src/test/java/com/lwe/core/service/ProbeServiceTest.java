package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProbeServiceTest {

    private final GameEntityRepository entityRepo = mock();
    private final WorldRepository worldRepo = mock();
    private final WorldAccess worldAccess = mock();
    private final ConditionEvaluator conditionEvaluator = mock();
    private final ModifierService modifierService = mock();
    private final RulesLoader rulesLoader = mock();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConditionService conditionService = new ConditionService(objectMapper);

    private ProbeService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID entityId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID gsId = UUID.randomUUID();

    private static final String D20_RULES = """
        {"version":1,"probeType":"d20_target","modifierFormula":"floor((attr-10)/2)",
         "attributes":[{"name":"staerke","type":"INT","min":1,"max":20,"default":10}],
         "skills":[{"name":"Athletik","attributes":["staerke"],"bonus":0}],
         "dice_mechanics":{"probe":"1d20+mod"}}
        """;

    @BeforeEach
    void setUp() {
        service = new ProbeService(entityRepo, worldRepo, worldAccess,
            conditionEvaluator, modifierService, rulesLoader, objectMapper, conditionService);
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
        lenient().when(conditionEvaluator.evaluate(any(), any())).thenReturn(java.util.List.of());
        lenient().when(modifierService.calculateModifiers(any(), any())).thenReturn(Map.of("staerke", 0.0));
        lenient().when(rulesLoader.loadRules(any(World.class))).thenAnswer(inv ->
            objectMapper.readValue(D20_RULES, Map.class));
    }

    private GameEntity entityWithAttrs(String attrs) {
        var e = new GameEntity(worldId, "PC", "Hero");
        e.setAttributesJson(attrs);
        try { var f = GameEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(e, entityId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        return e;
    }

    @Test
    void shouldUsePerCharacterSkillBonusOverGlobal() {
        // Entity with per-character skill value
        var entity = entityWithAttrs("{\"staerke\":10}");
        entity.setSkillsJson("{\"Athletik\":5}");

        var world = new World("W", userId, "{}");
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        var gs = new GameSystem("D20", 1, D20_RULES, "{}");
        try { var f = GameSystem.class.getDeclaredField("id"); f.setAccessible(true); f.set(gs, gsId); }
        catch (Exception ex) { throw new RuntimeException(ex); }

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        var result = service.executeProbe(entityId, userId, "Athletik", 15, false);

        // Global bonus = 0, per-character = 5
        // modifier = skillBonus(5) + attrMod(0) = 5
        // total = die(1-20) + 5, range 6-25
        assertThat(result.modifier()).isEqualTo(5);
        assertThat(result.total()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void shouldFallbackToGlobalBonusWhenNoPerCharacterSkill() {
        var entity = entityWithAttrs("{\"staerke\":10}");

        var world = new World("W", userId, "{}");
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        var gs = new GameSystem("D20", 1, D20_RULES, "{}");
        try { var f = GameSystem.class.getDeclaredField("id"); f.setAccessible(true); f.set(gs, gsId); }
        catch (Exception ex) { throw new RuntimeException(ex); }

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        var result = service.executeProbe(entityId, userId, "Athletik", 15, false);

        // Global bonus = 0, no per-character value
        // modifier = skillBonus(0) + attrMod(0) = 0
        // total = die(1-20), range 1-20
        assertThat(result.modifier()).isEqualTo(0);
        assertThat(result.total()).isBetween(1, 20);
    }

    @Test
    void shouldFallbackWhenSkillsJsonIsEmpty() {
        var entity = entityWithAttrs("{\"staerke\":10}");
        entity.setSkillsJson(null);

        var world = new World("W", userId, "{}");
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        var gs = new GameSystem("D20", 1, D20_RULES, "{}");
        try { var f = GameSystem.class.getDeclaredField("id"); f.setAccessible(true); f.set(gs, gsId); }
        catch (Exception ex) { throw new RuntimeException(ex); }

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        var result = service.executeProbe(entityId, userId, "Athletik", 15, false);

        assertThat(result.modifier()).isEqualTo(0);
        assertThat(result.total()).isBetween(1, 20);
    }

    @Test
    void conditionMalusAppliesToProbe() {
        var entity = entityWithAttrs("{\"staerke\":10}");
        entity.setMetadataJson("{\"conditions\":[{\"name\":\"Wunde\",\"rounds\":2}]}");
        var world = new World("W", userId, "{}");
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        when(entityRepo.findById(entityId)).thenReturn(java.util.Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv -> objectMapper.readValue("""
            {"version":1,"probeType":"d20_target","modifierFormula":"floor((attr-10)/2)",
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "skills":[{"name":"Athletik","attributes":["staerke"],"bonus":0}],
             "conditions":[{"name":"Wunde","effects":[{"target":"probe","op":"add","value":-4}]}],
             "dice_mechanics":{"probe":"1d20+mod"}}
            """, Map.class));

        var result = service.executeProbe(entityId, userId, "Athletik", 15, false);

        // attrMod 0 + bonus 0 + Zustands-Malus -4
        assertThat(result.modifier()).isEqualTo(-4);
    }
}
