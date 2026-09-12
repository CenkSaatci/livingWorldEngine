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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private final EntityService entityService = mock();
    private final RelationshipService relationshipService = mock();

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
            conditionEvaluator, modifierService, rulesLoader, objectMapper, conditionService,
            new DerivedValueService(), entityService, relationshipService);
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

    private static final String D100_RULES = """
        {"version":1,"probeType":"d100_threshold",
         "attributes":[{"name":"staerke","type":"INT","min":1,"max":99,"default":50}],
         "skills":[{"name":"Schiessen","attributes":["staerke"],"bonus":0}],
         "dice_mechanics":{"probe":"1d100","difficulties":[
           {"name":"hard","multiplier":0.001},{"name":"trivial","multiplier":1000}]}}
        """;

    private static final String D20_CAST_RULES = """
        {"version":1,"probeType":"d20_target",
         "attributes":[{"name":"staerke","type":"INT","min":1,"max":20,"default":10}],
         "skills":[{"name":"Feuerball","attributes":["staerke"],"bonus":0,
                    "casting":{"resource":"asp","cost":1,"requiresTrait":"Zauberer"}}],
         "derived_values":[{"name":"asp","formula":"5"}],
         "dice_mechanics":{"probe":"1d20"}}
        """;

    private static final String DSA_RULES = """
        {"version":1,"probeType":"d20_3attr",
         "attributes":[{"name":"mut","type":"INT","min":1,"max":20,"default":8},
                       {"name":"klugheit","type":"INT","min":1,"max":20,"default":8},
                       {"name":"intuition","type":"INT","min":1,"max":20,"default":8}],
         "skills":[{"name":"Odem","attributes":["klugheit","intuition","charisma"],"bonus":0,
                    "casting":{"resource":"asp","cost":2,"requiresTrait":"Zauberer"}}],
         "derived_values":[{"name":"asp","formula":"(mut+klugheit+intuition)/2"}],
         "dice_mechanics":{"probe":"3d20"}}
        """;

    @Test
    void dsaProbeShowsAdjustedThresholds() throws Exception {
        var entity = entityWithAttrs("{\"mut\":14,\"klugheit\":14,\"intuition\":13}");
        entity.setMetadataJson("{\"traits\":[\"Zauberer\"]}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(DSA_RULES, Map.class));

        var result = service.executeProbe(entityId, userId, "Odem", 0, false, campaignId(), 2);

        // Schwellen: klugheit 14-2=12, intuition 13-2=11, charisma default 10-2=8
        assertThat(result.details()).extracting(com.lwe.api.dto.ProbeResponse.DieDetail::attrValue)
            .containsExactly(12, 11, 8);
    }

    @Test
    void d100DifficultyMultiplierApplies() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":50}");
        entity.setSkillsJson("{\"Schiessen\":50}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D100_RULES, Map.class));

        var hard = service.executeProbe(entityId, userId, "Schiessen", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, "hard", 0, 0));
        var trivial = service.executeProbe(entityId, userId, "Schiessen", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, "trivial", 0, 0));

        assertThat(hard.success()).isFalse();
        assertThat(trivial.success()).isTrue();
    }

    @Test
    void d20DifficultyDeltaShiftsTarget() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D20_RULES, Map.class));

        // target 0 + Delta +100 => 1d20+0 reicht nie; Delta -100 => reicht immer.
        var impossible = service.executeProbe(entityId, userId, "Athletik", 0, false, campaignId(), 100);
        var trivial = service.executeProbe(entityId, userId, "Athletik", 0, false, campaignId(), -100);

        assertThat(impossible.success()).isFalse();
        assertThat(trivial.success()).isTrue();
    }

    @Test
    void d100BonusAndPenaltyCancelOut() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":50}");
        entity.setSkillsJson("{\"Schiessen\":50}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D100_RULES, Map.class));

        var result = service.executeProbe(entityId, userId, "Schiessen", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, null, 1, 1));

        assertThat(result.dice()).hasSize(2); // kein Netto-Extra => nur 1 Zehnerwurf
    }

    @Test
    void d100BonusDiceRollExtraTens() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":50}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D100_RULES, Map.class));

        var result = service.executeProbe(entityId, userId, "Schiessen", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, null, 1, 0));

        assertThat(result.dice()).hasSize(3); // 1 Einer + 2 Zehner (Grund + Bonus)
    }

    @Test
    void castUsesTargetForD20() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        entity.setMetadataJson("{\"traits\":[\"Zauberer\"]}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D20_CAST_RULES, Map.class));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Zielwert 100 ist mit 1d20 sicher nicht erreichbar -> Erfolg muss false sein.
        var result = service.cast(entityId, userId, "Feuerball", campaignId(), 100, null);

        assertThat(result.probe().success()).isFalse();
        assertThat(result.resourceRemaining()).isEqualTo(4);
    }

    @Test
    void castDeductsAsp() throws Exception {
        var entity = entityWithAttrs("{\"mut\":14,\"klugheit\":14,\"intuition\":13}");
        entity.setMetadataJson("{\"traits\":[\"Zauberer\"]}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(DSA_RULES, Map.class));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.cast(entityId, userId, "Odem", campaignId());

        assertThat(result.cost()).isEqualTo(2);
        assertThat(result.resourceRemaining()).isEqualTo(19);
        var saved = objectMapper.readTree(entity.getMetadataJson());
        assertThat(saved.path("asp_current").asInt()).isEqualTo(19);
    }

    @Test
    void castRejectsMissingTrait() throws Exception {
        var entity = entityWithAttrs("{\"mut\":14,\"klugheit\":14,\"intuition\":13}");
        entity.setMetadataJson("{}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(DSA_RULES, Map.class));

        assertThatThrownBy(() -> service.cast(entityId, userId, "Odem", campaignId()))
            .isInstanceOf(ProbeService.CastException.class)
            .matches(e -> ((ProbeService.CastException) e).getErrorCode().equals("CAST_MISSING_TRAIT"));
    }

    @Test
    void castRejectsInsufficientAsp() throws Exception {
        var entity = entityWithAttrs("{\"mut\":14,\"klugheit\":14,\"intuition\":13}");
        entity.setMetadataJson("{\"traits\":[\"Zauberer\"],\"asp_current\":1}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(DSA_RULES, Map.class));

        assertThatThrownBy(() -> service.cast(entityId, userId, "Odem", campaignId()))
            .isInstanceOf(ProbeService.CastException.class)
            .matches(e -> ((ProbeService.CastException) e).getErrorCode().equals("CAST_INSUFFICIENT_RESOURCE"));
    }

    @Test
    void castRejectsNonCastingSkill() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        lenient().when(rulesLoader.loadRules(any(), eq(worldId))).thenAnswer(inv ->
            objectMapper.readValue(D20_RULES, Map.class));

        assertThatThrownBy(() -> service.cast(entityId, userId, "Athletik", campaignId()))
            .isInstanceOf(ProbeService.CastException.class)
            .matches(e -> ((ProbeService.CastException) e).getErrorCode().equals("CAST_NOT_CASTABLE"));
    }

    private UUID campaignId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private void setWorldId(World world) {
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
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
    void dsaThreeAttrProbeCompensatesFailuresWithFw() throws Exception {
        // P29-T06 Abnahme: 3W20 mit FW-Ausgleich — FW 60 deckt maximal 3*19 ab.
        var entity = entityWithAttrs("{\"mut\":1,\"klugheit\":1,\"intuition\":1,\"staerke\":10}");
        entity.setSkillsJson("{\"Sinnesschärfe\":60}");
        var world = new World("W", userId, "{}");
        try { var f = World.class.getDeclaredField("id"); f.setAccessible(true); f.set(world, worldId); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        when(entityRepo.findById(entityId)).thenReturn(java.util.Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(java.util.Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv ->
            objectMapper.readValue(java.nio.file.Files.readString(
                java.nio.file.Path.of("../docs/examples/dsa5.json")), Map.class));

        var result = service.executeProbe(entityId, userId, "Sinnesschärfe", 10, false, null);

        assertThat(result.success()).as("FW 60 muss jede 3W20-Probe ausgleichen").isTrue();
        assertThat(result.dice()).hasSize(3);
    }

    @Test
    void useFateSpendsPointAndAddsBonus() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv -> objectMapper.readValue("""
            {"version":1,"probeType":"d20_target",
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "skills":[{"name":"Athletik","attributes":[],"bonus":0}],
             "fate":{"probeBonusPerPoint":2,"avoidDeathCost":1},
             "dice_mechanics":{"probe":"1d20+mod"}}
            """, Map.class));

        var result = service.executeProbe(entityId, userId, "Athletik", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, null, 0, 0, true));

        assertThat(result.modifier()).isEqualTo(2); // nur Fate-Bonus
        verify(entityService).spendFatePoint(entityId, userId, campaignId());
    }

    @Test
    void useFateWithoutFateConfigDoesNotSpend() {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        service.executeProbe(entityId, userId, "Athletik", 0, false, campaignId(),
            new ProbeService.ProbeOptions(0, null, 0, 0, true));

        verifyNoInteractions(entityService);
    }

    @Test
    void socialProbeAddsRelationshipModifierAndAppliesSuccessCondition() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var targetId = UUID.randomUUID();
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv -> objectMapper.readValue("""
            {"version":1,"probeType":"d20_target",
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "skills":[{"name":"Überreden","attributes":[],"bonus":0}],
             "social":{"relationshipScores":{"friendly":2,"hostile":-2},"maxModifier":3},
             "social_actions":[{"name":"Freundlich bitten","skill":"Überreden","relationshipWeight":1,
                "onSuccess":[{"condition":"Beeindruckt","rounds":3}],
                "onFailure":[{"condition":"Verärgert"}]}],
             "dice_mechanics":{"probe":"1d20+mod"}}
            """, Map.class));
        when(relationshipService.getRelationships(entityId)).thenReturn(java.util.List.of(
            new com.lwe.core.domain.EntityRelationship(entityId, targetId, "friendly")));
        var target = new GameEntity(worldId, "NPC", "Alrik");
        setId(target, targetId);
        when(entityRepo.findById(targetId)).thenReturn(Optional.of(target));

        // difficulty -100 => Erfolg immer
        var result = service.executeProbe(entityId, userId, "Überreden", 0, false, campaignId(),
            new ProbeService.ProbeOptions(-100, null, 0, 0), "Freundlich bitten", targetId);

        assertThat(result.success()).isTrue();
        assertThat(result.modifier()).isEqualTo(2); // Beziehung +2
        verify(entityService).applyCondition(eq(targetId), eq("Beeindruckt"), eq(3), any());
    }

    @Test
    void socialProbeCapsModifierAndAppliesFailureCondition() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var targetId = UUID.randomUUID();
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv -> objectMapper.readValue("""
            {"version":1,"probeType":"d20_target",
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "skills":[{"name":"Einschüchtern","attributes":[],"bonus":0}],
             "social":{"relationshipScores":{"hostile":-2},"maxModifier":3},
             "social_actions":[{"name":"Drohen","skill":"Einschüchtern","relationshipWeight":2,
                "onFailure":[{"condition":"Verängstigt"}]}],
             "dice_mechanics":{"probe":"1d20+mod"}}
            """, Map.class));
        when(relationshipService.getRelationships(entityId)).thenReturn(java.util.List.of(
            new com.lwe.core.domain.EntityRelationship(targetId, entityId, "hostile")));
        var target = new GameEntity(worldId, "NPC", "Alrik");
        setId(target, targetId);
        when(entityRepo.findById(targetId)).thenReturn(Optional.of(target));

        // difficulty +1000 => Fehlschlag immer; -2 * 2 = -4 wird auf -3 gedeckelt
        var result = service.executeProbe(entityId, userId, "Einschüchtern", 0, false, campaignId(),
            new ProbeService.ProbeOptions(1000, null, 0, 0), "Drohen", targetId);

        assertThat(result.success()).isFalse();
        assertThat(result.modifier()).isEqualTo(-3);
        verify(entityService).applyCondition(eq(targetId), eq("Verängstigt"), eq(null), any());
    }

    @Test
    void socialProbeRejectsUnknownAction() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv ->
            objectMapper.readValue(D20_RULES, Map.class));

        assertThatThrownBy(() -> service.executeProbe(entityId, userId, "Athletik", 0, false,
            campaignId(), new ProbeService.ProbeOptions(0, null, 0, 0), "Nix", null))
            .isInstanceOf(ProbeService.SocialException.class)
            .matches(e -> ((ProbeService.SocialException) e).getErrorCode().equals("SOCIAL_ACTION_UNKNOWN"));
    }

    @Test
    void socialProbeRejectsForeignWorldTargetAndWrongSkill() throws Exception {
        var entity = entityWithAttrs("{\"staerke\":10}");
        var world = new World("W", userId, "{}");
        setWorldId(world);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(rulesLoader.loadRules(any(), any())).thenAnswer(inv -> objectMapper.readValue("""
            {"version":1,"probeType":"d20_target",
             "attributes":[{"name":"staerke","type":"INT","default":10}],
             "skills":[{"name":"Überreden","attributes":[],"bonus":0}],
             "social_actions":[{"name":"Freundlich bitten","skill":"Überreden"}]}
            """, Map.class));

        // falscher Skill -> SOCIAL_SKILL_MISMATCH
        assertThatThrownBy(() -> service.executeProbe(entityId, userId, "Athletik", 0, false,
            campaignId(), new ProbeService.ProbeOptions(0, null, 0, 0), "Freundlich bitten", UUID.randomUUID()))
            .isInstanceOf(ProbeService.SocialException.class)
            .matches(e -> ((ProbeService.SocialException) e).getErrorCode().equals("SOCIAL_SKILL_MISMATCH"));

        // Ziel nicht in dieser Welt/findet nichts -> SOCIAL_TARGET_INVALID
        var foreignTarget = new GameEntity(UUID.randomUUID(), "NPC", "Fremd");
        setId(foreignTarget, UUID.randomUUID());
        when(entityRepo.findById(foreignTarget.getId())).thenReturn(Optional.of(foreignTarget));
        assertThatThrownBy(() -> service.executeProbe(entityId, userId, "Überreden", 0, false,
            campaignId(), new ProbeService.ProbeOptions(0, null, 0, 0), "Freundlich bitten", foreignTarget.getId()))
            .isInstanceOf(ProbeService.SocialException.class)
            .matches(e -> ((ProbeService.SocialException) e).getErrorCode().equals("SOCIAL_TARGET_INVALID"));
    }

    private void setId(Object obj, java.util.UUID id) {
        try {
            var f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void conditionMalusAppliesToProbe() {        var entity = entityWithAttrs("{\"staerke\":10}");
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
