package com.lwe.core.service;

import com.lwe.core.domain.*;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdventureServiceTest {

    private final AdventureRepository adventureRepo = mock();
    private final AdventureNodeRepository nodeRepo = mock();
    private final NodeChoiceRepository choiceRepo = mock();
    private final AdventureProgressRepository progressRepo = mock();
    private final WorldRepository worldRepo = mock();
    private final RollService rollService = mock();
    private final WorldEventService eventService = mock();
    private final WorldAccess worldAccess = mock();

    private AdventureService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID startNodeId = UUID.randomUUID();
    private final Adventure adventure;

    AdventureServiceTest() {
        adventure = new Adventure(worldId, "Test Adventure");
        setId(adventure, UUID.randomUUID());
        adventure.setStartNodeId(startNodeId);
    }

    @BeforeEach
    void setUp() {
        service = new AdventureService(adventureRepo, nodeRepo, choiceRepo, progressRepo,
            worldRepo, rollService, eventService, worldAccess);
    }

    @Test
    void shouldCreateAdventure() {
        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(adventureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createAdventure(worldId, userId, "Quest", null);
        assertThat(result.getName()).isEqualTo("Quest");
    }

    @Test
    void shouldStartAdventure() {
        var entityId = UUID.randomUUID();
        var advNode = new AdventureNode(adventure.getId(), "Start", false);
        setId(advNode, startNodeId);
        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
        setId(world, worldId);

        when(adventureRepo.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(progressRepo.findByAdventureIdAndEntityId(adventure.getId(), entityId)).thenReturn(Optional.empty());
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var progress = service.start(adventure.getId(), entityId, userId);
        assertThat(progress.getStatus()).isEqualTo("ACTIVE");
        assertThat(progress.getCurrentNodeId()).isEqualTo(startNodeId);
    }

    @Test
    void shouldAdvanceThroughChoice() {
        var entityId = UUID.randomUUID();
        var node2Id = UUID.randomUUID();
        var node2 = new AdventureNode(adventure.getId(), "Middle", false);
        setId(node2, node2Id);

        var choice = new NodeChoice(startNodeId, "Go north", node2Id);
        setId(choice, UUID.randomUUID());
        var progress = new AdventureProgress(adventure.getId(), entityId, startNodeId);
        setId(progress, UUID.randomUUID());
        var world = new com.lwe.core.domain.World("W", userId, null, "{}");
        setId(world, worldId);

        when(adventureRepo.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(progressRepo.findByAdventureIdAndEntityId(adventure.getId(), entityId)).thenReturn(Optional.of(progress));
        when(choiceRepo.findById(choice.getId())).thenReturn(Optional.of(choice));
        when(nodeRepo.findById(node2Id)).thenReturn(Optional.of(node2));
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = service.advance(adventure.getId(), entityId, choice.getId(), userId);
        assertThat(result.completed()).isFalse();
        assertThat(result.nextNode().getId()).isEqualTo(node2Id);
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}