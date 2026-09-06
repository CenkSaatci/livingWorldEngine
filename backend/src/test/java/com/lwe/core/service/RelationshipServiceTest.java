package com.lwe.core.service;

import com.lwe.core.domain.EntityRelationship;
import com.lwe.core.repository.EntityRelationshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock private EntityRelationshipRepository repo;

    @InjectMocks private RelationshipService relationshipService;

    @Test
    void shouldCreateRelationship() {
        var aId = UUID.randomUUID();
        var bId = UUID.randomUUID();

        when(repo.findByEntityAIdAndEntityBId(aId, bId)).thenReturn(Optional.empty());
        when(repo.findByEntityAIdAndEntityBId(bId, aId)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.<EntityRelationship>getArgument(0));

        var result = relationshipService.setRelationship(aId, bId, "friend");

        assertThat(result.getRelationship()).isEqualTo("friend");
        verify(repo).save(any());
    }

    @Test
    void shouldUpdateExistingRelationship() {
        var aId = UUID.randomUUID();
        var bId = UUID.randomUUID();
        var existing = new EntityRelationship(aId, bId, "neutral");

        when(repo.findByEntityAIdAndEntityBId(aId, bId)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.<EntityRelationship>getArgument(0));

        var result = relationshipService.setRelationship(aId, bId, "friend");

        assertThat(result.getRelationship()).isEqualTo("friend");
        verify(repo).save(any());
    }

    @Test
    void shouldGetRelationshipsByEntityId() {
        var entityId = UUID.randomUUID();
        var rel = new EntityRelationship(entityId, UUID.randomUUID(), "ally");
        when(repo.findByEntityAIdOrEntityBId(entityId, entityId)).thenReturn(List.of(rel));

        var result = relationshipService.getRelationships(entityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelationship()).isEqualTo("ally");
    }
}
