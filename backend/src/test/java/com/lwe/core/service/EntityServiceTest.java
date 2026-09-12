package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.repository.GameEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {
    @Mock
    private GameEntityRepository entityRepo;
    @Mock
    private WorldAccess worldAccess;
    @Mock
    private RulesLoader rulesLoader;
    @Mock
    private ConditionService conditionService;
    @Mock
    private DerivedValueService derivedValueService;
    @Mock
    private com.lwe.core.repository.CampaignRepository campaignRepo;
    private EntityService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EntityService(entityRepo, worldAccess, new ObjectMapper(), rulesLoader, conditionService,
            derivedValueService, new com.lwe.core.util.EntityAccess(entityRepo, worldAccess), campaignRepo);
    }

    @Test
    void createInitializesHpFromLep() {
        var campaignId = UUID.randomUUID();
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(true);
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of("derived_values", List.of()));
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(
            List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("lep", 15.5, null)));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(worldId, userId, "PC", "Held",
            "{\"konstitution\":11,\"koerperkraft\":10}", null, null, null, null,
            null, null, null, null, campaignId, null);

        assertThat(result.getHpMax()).isEqualTo(16);
        assertThat(result.getHpCurrent()).isEqualTo(16);
    }

    @Test
    void createPersistsSkillsJson() {
        var campaignId = UUID.randomUUID();
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(true);
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of());
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(List.of());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(worldId, userId, "PC", "Held",
            "{\"mut\":14}", null, null, null, null,
            null, null, null, null, campaignId, "{\"Sinnesschärfe\":4}");

        assertThat(result.getSkillsJson()).contains("Sinnesschärfe");
    }

    @Test
    void updateAttributesRecalculatesHpKeepingDamage() {
        var campaignId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var entity = new GameEntity(worldId, "PC", "Held");
        entity.setAttributesJson("{\"konstitution\":11,\"koerperkraft\":10}");
        entity.setHpMax(16);
        entity.setHpCurrent(14);
        try {
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, entityId);
        } catch (Exception e) { throw new RuntimeException(e); }
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(true);
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of());
        when(derivedValueService.evaluate(any(), any(), any())).thenReturn(
            List.of(new com.lwe.api.dto.SheetResponse.DerivedValueInfo("lep", 17.0, null)));
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateAttributes(entityId, userId, Map.of("konstitution", 13), campaignId);

        assertThat(result.getHpMax()).isEqualTo(17);
        assertThat(result.getHpCurrent()).isEqualTo(15);
    }

    @Test
    void createWithoutCampaignKeepsDefaultHp() {
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(worldId, userId, "NPC", "Goblin",
            null, null, null, null, null,
            null, null, null, null, null, null);

        assertThat(result.getHpMax()).isEqualTo(10);
        verifyNoInteractions(derivedValueService);
    }

    @Test
    void createRejectsForeignCampaign() {
        var campaignId = UUID.randomUUID();
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(worldId, userId, "PC", "Held",
                null, null, null, null, null,
                null, null, null, null, campaignId, null))
            .isInstanceOf(EntityService.EntityException.class);
    }

    @Test
    void shouldCreateEntity() {
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(entityRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<GameEntity>getArgument(0);
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, UUID.randomUUID());
            return e;
        });

        var result = service.create(worldId, userId, "NPC", "Goblin",
            null, null, null, null, null,
            null, null, null, null, null, null);

        assertThat(result.getName()).isEqualTo("Goblin");
        assertThat(result.getEntityType()).isEqualTo("NPC");
        verify(entityRepo).save(any());
    }

    @Test
    void blankPositionJsonIsStoredAsNull() {
        doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(worldId, userId, "NPC", "Goblin",
            "{\"staerke\":10}", "", "", null, null,
            null, null, null, null, null, null);

        assertThat(result.getPositionJson()).isNull();
    }

    @Test
    void shouldThrowOnMissingEntity() {        when(entityRepo.findById(any())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), userId))
            .isInstanceOf(EntityService.EntityException.class)
            .matches(e -> ((EntityService.EntityException) e).getErrorCode().equals("ENTITY_NOT_FOUND"));
    }

    private GameEntity entityWithId(String attrsJson) {
        var entity = new GameEntity(worldId, "PC", "Held");
        try {
            var f = GameEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
        entity.setAttributesJson(attrsJson);
        return entity;
    }

    @Test
    void skillAboveMaxIsRejected() {
        var entity = entityWithId("{\"staerke\":14}");
        var campaignId = campaignInWorld();
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "skills", List.of(Map.of("name", "Athletik", "attributes", List.of("staerke"))),
            "advancement", Map.of("maxRule", "highestAttributePlus2")));

        // Max = hoechstes beteiligtes Attribut (14) + 2 = 16; 20 ist zu hoch.
        assertThatThrownBy(() -> service.updateSkills(entity.getId(), userId,
            Map.of("Athletik", 20), campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("SKILL_MAX_EXCEEDED"));
    }

    @Test
    void skillAtMaxIsAccepted() {
        var entity = entityWithId("{\"staerke\":14}");
        var campaignId = campaignInWorld();
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "skills", List.of(Map.of("name", "Athletik", "attributes", List.of("staerke"))),
            "advancement", Map.of("maxRule", "highestAttributePlus2")));

        var result = service.updateSkills(entity.getId(), userId, Map.of("Athletik", 16), campaignId);

        assertThat(result.getSkillsJson()).contains("\"Athletik\":16");
    }

    @Test
    void skillMaxNotEnforcedWithoutCampaign() {
        var entity = entityWithId("{\"staerke\":14}");
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateSkills(entity.getId(), userId, Map.of("Athletik", 20));

        assertThat(result.getSkillsJson()).contains("\"Athletik\":20");
        verifyNoInteractions(rulesLoader);
    }

    private UUID campaignInWorld() {
        var campaignId = UUID.randomUUID();
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(true);
        return campaignId;
    }

    @Test
    void skillMaxUsesRulesDefaultsWhenEntityHasNoAttributes() {
        // Audit P28: frischer Charakter (keine gespeicherten Attribute) darf die
        // Max-Regel nicht umgehen — Defaults aus rulesJson zaehlen.
        var entity = entityWithId(null);
        var campaignId = campaignInWorld();
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "attributes", List.of(Map.of("name", "staerke", "default", 10)),
            "skills", List.of(Map.of("name", "Athletik", "attributes", List.of("staerke"))),
            "advancement", Map.of("maxRule", "highestAttributePlus2")));

        // Default 10 → Max 12; 13 muss abgelehnt werden.
        assertThatThrownBy(() -> service.updateSkills(entity.getId(), userId,
            Map.of("Athletik", 13), campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("SKILL_MAX_EXCEEDED"));
    }

    @Test
    void skillMaxReadsLegacySingularAttribute() {
        // Audit P28: Seeds nutzen "attribute" (Singular) — darf die Regel nicht umgehen.
        var entity = entityWithId("{\"staerke\":14}");
        var campaignId = campaignInWorld();
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "skills", List.of(Map.of("name", "Athletik", "attribute", "staerke")),
            "advancement", Map.of("maxRule", "highestAttributePlus2")));

        assertThatThrownBy(() -> service.updateSkills(entity.getId(), userId,
            Map.of("Athletik", 20), campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("SKILL_MAX_EXCEEDED"));
    }

    @Test
    void foreignCampaignIdIsRejectedInsteadOfSilentlySkipping() {
        // Audit P28: fremde campaignId deaktivierte den Cap still.
        var entity = entityWithId("{\"staerke\":14}");
        var campaignId = UUID.randomUUID();
        when(entityRepo.findById(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(rulesLoader.campaignBelongsToWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateSkills(entity.getId(), userId,
            Map.of("Athletik", 5), campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("WORLD_ACCESS_DENIED"));
    }

    @Test
    void addConditionValidatesCatalogAndPersists() {
        var entity = entityWithId("{\"staerke\":10}");
        var campaignId = campaignInWorld();
        when(entityRepo.findByIdForUpdate(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "conditions", List.of(Map.of("name", "Wunde"))));

        service.addCondition(entity.getId(), userId, "Wunde", 2, campaignId);

        verify(conditionService).add(eq(entity), any());
    }

    @Test
    void addConditionDefaultsRoundsFromCatalog() {
        var entity = entityWithId("{\"staerke\":10}");
        var campaignId = campaignInWorld();
        when(entityRepo.findByIdForUpdate(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "conditions", List.of(Map.of("name", "Wunde", "rounds", 3))));

        service.addCondition(entity.getId(), userId, "Wunde", null, campaignId);

        verify(conditionService).add(eq(entity), org.mockito.ArgumentMatchers.argThat(
            c -> c.rounds() != null && c.rounds() == 3));
    }

    @Test
    void addConditionRejectsUnknownName() {
        var entity = entityWithId("{\"staerke\":10}");
        var campaignId = campaignInWorld();
        when(entityRepo.findByIdForUpdate(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "conditions", List.of(Map.of("name", "Wunde"))));

        assertThatThrownBy(() -> service.addCondition(entity.getId(), userId, "Nix", null, campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("UNKNOWN_CONDITION"));
    }

    @Test
    void removeConditionDelegates() {
        var entity = entityWithId("{\"staerke\":10}");
        when(entityRepo.findByIdForUpdate(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.removeCondition(entity.getId(), userId, "Wunde");

        verify(conditionService).remove(entity, "Wunde");
    }

    @Test
    void spendFatePointDecrementsAndRejectsAtZero() {
        var entity = entityWithId("{\"staerke\":10}");
        var campaignId = campaignInWorld();
        when(entityRepo.findByIdForUpdate(any())).thenReturn(Optional.of(entity));
        doNothing().when(worldAccess).requireAccess(any(), any());
        when(entityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rulesLoader.loadRules(campaignId, worldId)).thenReturn(Map.of(
            "creationBudget", Map.of("fatePoints", 2)));

        service.spendFatePoint(entity.getId(), userId, campaignId); // 2 -> 1
        assertThat(service.fatePoints(entity, 2)).isEqualTo(1);

        service.spendFatePoint(entity.getId(), userId, campaignId); // 1 -> 0
        assertThatThrownBy(() -> service.spendFatePoint(entity.getId(), userId, campaignId))
            .isInstanceOf(EntityService.EntityException.class)
            .satisfies(e -> assertThat(((EntityService.EntityException) e).getErrorCode())
                .isEqualTo("FATE_NONE_LEFT"));
    }
}
