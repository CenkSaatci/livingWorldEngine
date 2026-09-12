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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lwe.core.util.WorldAccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestServiceTest {

    @Mock private GameEntityRepository entityRepo;
    @Mock private WorldAccess worldAccess;
    @Mock private RulesLoader rulesLoader;
    @Mock private DerivedValueService derivedValueService;

    private RestService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RestService(entityRepo, worldAccess,
            new com.lwe.core.util.EntityAccess(entityRepo, worldAccess), rulesLoader, new ObjectMapper(),
            derivedValueService);
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

    private void stubSystem(String rulesJson) {
        var gs = new GameSystem("D20", 1, rulesJson.startsWith("{") ? rulesJson : "{\"version\":1,\"attributes\":[]," + rulesJson + "}", "{}");
        setId(gs, gameSystemId);
        when(rulesLoader.loadSystem(any(UUID.class))).thenReturn(gs);
    }

    private static final String CAST_REST_RULES = """
        {"version":1,
         "attributes":[{"name":"mut","type":"INT","min":1,"max":20,"default":10}],
         "skills":[{"name":"Odem","attributes":["mut"],"bonus":0,
                    "casting":{"resource":"slp","cost":1,"restore":"long"}}],
         "derived_values":[{"name":"slp","formula":"3"}],
         "dice_mechanics":{"probe":"1d20","combat":{
           "initiative":"1d20","damage":"1d8",
           "resting":{"long_rest":{"full_heal":true,"recover_all":true}}}}}
        """;

    private static final String SHORT_CAST_RULES = """
        {"version":1,
         "attributes":[{"name":"mut","type":"INT","min":1,"max":20,"default":10}],
         "skills":[{"name":"Fokus","attributes":["mut"],"bonus":0,
                    "casting":{"resource":"fokus","cost":1,"restore":"short"}}],
         "derived_values":[{"name":"fokus","formula":"2"}],
         "dice_mechanics":{"probe":"1d20","combat":{
           "initiative":"1d20","damage":"1d8",
           "resting":{"short_rest":{"recover_resources":true}}}}}
        """;

    @Test
    void shortRestRestoresShortResourcesOnly() {
        var entity = entityWithHp(5, 10, 0, 2);
        entity.setAttributesJson("{\"mut\":10}");
        entity.setMetadataJson("{\"fokus_current\":0}");
        stubSystem(SHORT_CAST_RULES);
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(
            List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("fokus", 2.0, null)));
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getMetadataJson()).contains("\"fokus_current\":2");
    }

    @Test
    void longRestRestoresCastResourcesGenerically() {
        var entity = entityWithHp(5, 10, 0, 2);
        entity.setAttributesJson("{\"mut\":10}");
        entity.setMetadataJson("{\"slp_current\":0}");
        stubSystem(CAST_REST_RULES);
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(
            List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("slp", 3.0, null)));
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.longRest(entity.getId(), userId);

        assertThat(entity.getMetadataJson()).contains("\"slp_current\":3");
    }

    @Test
    void shortRestUsesCampaignRulesFirst() {
        var campaignId = UUID.randomUUID();
        var entity = entityWithHp(10, 50, 0, 2);
        var gs = new GameSystem("D20", 1,
            "{\"version\":1,\"attributes\":[]," + NEW_REST_CONFIG + "}", "{}");
        setId(gs, gameSystemId);
        when(rulesLoader.loadSystemByCampaign(campaignId)).thenReturn(gs);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId, campaignId);

        assertThat(entity.getHpCurrent()).isEqualTo(35); // 10 + 50% of 50 = 35
    }

    @Test
    void shortRestHealsPercentageHp() {
        var entity = entityWithHp(10, 50, 0, 2);
        stubSystem(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(35); // 10 + 50% of 50 = 35
    }

    @Test
    void shortRestRestoresAp() {
        var entity = entityWithHp(50, 50, 0, 2);
        stubSystem(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getApCurrent()).isEqualTo(2); // restored to max
    }

    @Test
    void longRestFullyHeals() {
        var entity = entityWithHp(10, 50, 0, 2);
        stubSystem(NEW_REST_CONFIG);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.longRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(50);
        assertThat(entity.getApCurrent()).isEqualTo(2);
    }

    @Test
    void shortRestWithDiceExpression() {
        var entity = entityWithHp(10, 50, 0, 2);
        stubSystem("""
            "dice_mechanics":{"probe":"1d20","combat":{
              "initiative":"1d20","damage":"1d8",
              "resting":{"short_rest":{"hp":"1d6","ap":null,"recover":[]}}
            }}
            """);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isBetween(11, 16); // 10 + (1 to 6)
    }

    @Test
    void restWithoutConfigDefaultsToNoop() {
        var entity = entityWithHp(5, 20, 0, 2);
        stubSystem("""
            {"version":1,"attributes":[],"dice_mechanics":{"probe":"1d20"}}
            """);
        when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));

        service.shortRest(entity.getId(), userId);

        assertThat(entity.getHpCurrent()).isEqualTo(5); // unchanged
    }

    @Test
    void restNeverReducesHpWhenCurrentExceedsMax() {
        // current > max (z.B. nach max-Senkung): Rest darf HP nicht senken
        for (var hp : List.of("50%", "5", "1d6")) {
            var entity = entityWithHp(60, 50, 0, 2);
            stubSystem("""
                "dice_mechanics":{"probe":"1d20","combat":{
                  "initiative":"1d20","damage":"1d8",
                  "resting":{"short_rest":{"hp":"HP","ap":null,"recover":[]}}
                }}
                """.replace("HP", hp));
            when(entityRepo.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.shortRest(entity.getId(), userId);

            assertThat(entity.getHpCurrent())
                .as("hp expr %s bei current>max", hp)
                .isEqualTo(60);
        }
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

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
