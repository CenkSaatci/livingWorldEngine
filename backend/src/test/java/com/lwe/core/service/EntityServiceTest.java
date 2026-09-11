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
    private EntityService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EntityService(entityRepo, worldAccess, new ObjectMapper(), rulesLoader);
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
            null, null, null, null);

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
            null, null, null, null);

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
        var campaignId = UUID.randomUUID();
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
        var campaignId = UUID.randomUUID();
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
}
