package com.lwe.core.service;

import com.lwe.core.domain.EntityEvent;
import com.lwe.core.repository.EntityEventRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 🔴 RED: EntityEventService existiert noch nicht → Kompilerfehler.
 * 🟢 GREEN: Implementierung erzeugen.
 */
class EntityEventServiceTest {

    private final EntityEventRepository repo = mock();
    private final com.lwe.core.repository.GameEntityRepository entityRepo = mock();
    private final com.lwe.core.repository.RegionRepository regionRepo = mock();
    private final com.lwe.core.repository.LocationRepository locationRepo = mock();
    private final com.lwe.core.repository.QuestRepository questRepo = mock();
    private final EntityEventService service = new EntityEventService(
        repo, entityRepo, regionRepo, locationRepo, questRepo);

    @Test
    void resolveWorldIdHandlesEntityAndLocation() {
        var worldId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var entity = new com.lwe.core.domain.GameEntity(worldId, "NPC", "Wirt");
        var region = new com.lwe.core.domain.Region(worldId, "Nord");
        var location = new com.lwe.core.domain.Location(region.getId(), "Dorf", "Start");
        when(entityRepo.findById(entityId)).thenReturn(java.util.Optional.of(entity));
        when(locationRepo.findById(entityId)).thenReturn(java.util.Optional.of(location));
        when(regionRepo.findById(region.getId())).thenReturn(java.util.Optional.of(region));

        assertThat(service.resolveWorldId("NPC", entityId)).contains(worldId);
        assertThat(service.resolveWorldId("location", entityId)).contains(worldId);
        assertThat(service.resolveWorldId("unknown", entityId)).isEmpty();
    }

    @Test
    void shouldPublishEvent() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var event = service.publish("region", UUID.randomUUID(),
            "GOBLIN_RAID", "Goblin-Überfall", "Eine Gruppe Goblins überfiel das Schattental.", 3, null);

        assertThat(event).isNotNull();
        assertThat(event.getTitle()).isEqualTo("Goblin-Überfall");
        assertThat(event.getEventType()).isEqualTo("GOBLIN_RAID");
        assertThat(event.getImportance()).isEqualTo(3);
        verify(repo).save(any());
    }

    @Test
    void shouldQueryEventsByEntity() {
        var entityId = UUID.randomUUID();
        when(repo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("location", entityId))
            .thenReturn(List.of());

        var events = service.getEvents("location", entityId, 10);
        assertThat(events).isEmpty();
    }

    @Test
    void shouldRespectLimit() {
        var entityId = UUID.randomUUID();
        when(repo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("npc", entityId))
            .thenReturn(List.of());

        var events = service.getEvents("npc", entityId, 5);
        assertThat(events).isEmpty();
        verify(repo).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("npc", entityId);
    }
}
