package com.lwe.core.service;

import com.lwe.core.domain.WorldEvent;
import com.lwe.core.repository.WorldEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldEventServiceTest {

    @Mock private WorldEventRepository eventRepo;
    @Mock private SimpMessagingTemplate messaging;

    private WorldEventService service;

    @BeforeEach
    void setUp() {
        service = new WorldEventService(eventRepo, messaging);
    }

    @Test
    void publishShouldSaveEventAndReturnId() throws Exception {
        var worldId = UUID.randomUUID();
        when(eventRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<WorldEvent>getArgument(0);
            var idField = WorldEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, 42L);
            return e;
        });

        var id = service.publish(worldId, WorldEventService.EventType.PROBE_ROLLED,
            UUID.randomUUID(), UUID.randomUUID(), Map.of("key", "value"));

        assertThat(id).isEqualTo(42L);
        verify(eventRepo).save(any());
    }

    @Test
    void publishShouldHandleMissingMessagingTemplate() throws Exception {
        var serviceNoWs = new WorldEventService(eventRepo, null);
        when(eventRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<WorldEvent>getArgument(0);
            var idField = WorldEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, 7L);
            return e;
        });

        var id = serviceNoWs.publish(UUID.randomUUID(), WorldEventService.EventType.TIME_ADVANCED,
            null, null, Map.of());

        assertThat(id).isEqualTo(7L);
    }

    @Test
    void eventTypeShouldContainExpectedValues() {
        assertThat(WorldEventService.EventType.values())
            .contains(WorldEventService.EventType.PROBE_ROLLED)
            .contains(WorldEventService.EventType.COMBAT_STARTED)
            .contains(WorldEventService.EventType.WEATHER_CHANGED)
            .contains(WorldEventService.EventType.CHAT_MESSAGE)
            .contains(WorldEventService.EventType.SESSION_STARTED)
            .contains(WorldEventService.EventType.TIME_ADVANCED);
    }
}
