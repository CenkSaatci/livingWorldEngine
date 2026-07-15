package com.lwe.core.service;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.EntityAbility;
import com.lwe.core.repository.EntityAbilityRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.AbilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityAbilityServiceTest {

    @Mock private EntityAbilityRepository repo;
    @Mock private GameEntityRepository entityRepo;
    @Mock private AbilityRepository abilityRepo;

    private EntityAbilityService service;
    private final UUID entityId = UUID.randomUUID();
    private final UUID abilityId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EntityAbilityService(repo, entityRepo, abilityRepo);
    }

    @Test
    void shouldAssignAbility() {
        var entity = new GameEntity(worldId, "PC", "Aragorn");
        setId(entity, entityId);
        var ability = new Ability(worldId, "Feuerball", AbilityType.ACTIVE);
        setId(ability, abilityId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(abilityRepo.findById(abilityId)).thenReturn(Optional.of(ability));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assign(entityId, abilityId);

        verify(repo).save(argThat(ea ->
            ea.getEntityId().equals(entityId) && ea.getAbilityId().equals(abilityId)));
    }

    @Test
    void shouldRejectAssignFromDifferentWorld() {
        var entity = new GameEntity(worldId, "PC", "Aragorn");
        setId(entity, entityId);
        var ability = new Ability(UUID.randomUUID(), "Wrong", AbilityType.ACTIVE);
        setId(ability, abilityId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(abilityRepo.findById(abilityId)).thenReturn(Optional.of(ability));

        assertThatThrownBy(() -> service.assign(entityId, abilityId))
            .isInstanceOf(EntityAbilityService.EntityAbilityException.class);
    }

    @Test
    void shouldListAssignedAbilities() {
        when(repo.findByEntityId(entityId)).thenReturn(java.util.List.of());

        var list = service.listByEntity(entityId);
        assertThat(list).isEmpty();
        verify(repo).findByEntityId(entityId);
    }

    @Test
    void shouldRemoveAssignment() {
        var entity = new GameEntity(worldId, "PC", "Aragorn");
        setId(entity, entityId);

        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        when(repo.findByEntityIdAndAbilityId(entityId, abilityId)).thenReturn(Optional.of(
            new com.lwe.core.domain.EntityAbility(entityId, abilityId)));

        service.unassign(entityId, abilityId);

        verify(repo).delete(any());
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
