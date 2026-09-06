package com.lwe.core.service;

import com.lwe.core.domain.Faction;
import com.lwe.core.domain.FactionRelation;
import com.lwe.core.repository.FactionRelationRepository;
import com.lwe.core.repository.FactionRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactionServiceTest {

    @Mock private FactionRepository factionRepo;
    @Mock private FactionRelationRepository relationRepo;
    @Mock private WorldAccess worldAccess;

    @InjectMocks private FactionService factionService;

    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldCreateFaction() {
        when(factionRepo.save(any())).thenAnswer(inv -> {
            var f = inv.<Faction>getArgument(0);
            setId(f, UUID.randomUUID());
            return f;
        });

        var result = factionService.create(worldId, userId, "name", "desc", "#ff0000", null);

        assertThat(result.getName()).isEqualTo("name");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getColor()).isEqualTo("#ff0000");
        assertThat(result.getWorldId()).isEqualTo(worldId);
        verify(factionRepo).save(any());
    }

    @Test
    void shouldListFactions() {
        var faction = factionWithId(worldId, "A");
        when(factionRepo.findByWorldIdOrderByNameAsc(worldId)).thenReturn(List.of(faction));

        var result = factionService.list(worldId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("A");
    }

    @Test
    void shouldThrowWhenFactionNotFound() {
        when(factionRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factionService.getById(UUID.randomUUID(), userId))
            .isInstanceOf(FactionService.FactionException.class)
            .matches(e -> ((FactionService.FactionException) e).getErrorCode().equals("FACTION_NOT_FOUND"));
    }

    @Test
    void shouldUpdateFaction() {
        var faction = factionWithId(worldId, "old");
        var factionId = faction.getId();

        when(factionRepo.findById(factionId)).thenReturn(Optional.of(faction));
        when(factionRepo.save(any())).thenAnswer(inv -> inv.<Faction>getArgument(0));

        var result = factionService.update(factionId, userId, "new", null, null, null);

        assertThat(result.getName()).isEqualTo("new");
        verify(factionRepo).save(any());
    }

    @Test
    void shouldDeleteFaction() {
        var faction = factionWithId(worldId, "del");
        var factionId = faction.getId();

        when(factionRepo.findById(factionId)).thenReturn(Optional.of(faction));

        factionService.delete(factionId, userId);

        verify(factionRepo).delete(faction);
    }

    @Test
    void shouldSetRelation() {
        var a = factionWithId(worldId, "A");
        var b = factionWithId(worldId, "B");

        when(factionRepo.findById(a.getId())).thenReturn(Optional.of(a));
        when(factionRepo.findById(b.getId())).thenReturn(Optional.of(b));
        when(relationRepo.findByFactionAIdAndFactionBId(a.getId(), b.getId())).thenReturn(Optional.empty());
        when(relationRepo.findByFactionAIdAndFactionBId(b.getId(), a.getId())).thenReturn(Optional.empty());
        when(relationRepo.save(any())).thenAnswer(inv -> inv.<FactionRelation>getArgument(0));

        var result = factionService.setRelation(a.getId(), b.getId(), "ALLY", userId);

        assertThat(result.getRelationStatus()).isEqualTo("ALLY");
        verify(relationRepo).save(any());
    }

    @Test
    void shouldSetRelationInternal() {
        when(relationRepo.findByFactionAIdAndFactionBId(any(), any())).thenReturn(Optional.empty());
        when(relationRepo.save(any())).thenAnswer(inv -> inv.<FactionRelation>getArgument(0));

        var aId = UUID.randomUUID();
        var bId = UUID.randomUUID();

        var result = factionService.setRelationInternal(aId, bId, "WAR");

        assertThat(result.getRelationStatus()).isEqualTo("WAR");
        verify(relationRepo).save(any());
    }

    @Test
    void shouldGetRelations() {
        var factionId = UUID.randomUUID();
        var relation = new FactionRelation(factionId, UUID.randomUUID(), "ALLY");
        when(relationRepo.findByFactionAIdOrFactionBId(factionId, factionId))
            .thenReturn(List.of(relation));

        var result = factionService.getRelations(factionId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelationStatus()).isEqualTo("ALLY");
    }

    private Faction factionWithId(UUID worldId, String name) {
        var f = new Faction(worldId, name);
        setId(f, UUID.randomUUID());
        return f;
    }

    private void setId(Object obj, UUID id) {
        try {
            var field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
