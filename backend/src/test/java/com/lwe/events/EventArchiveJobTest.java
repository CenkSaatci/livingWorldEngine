package com.lwe.events;

import com.lwe.core.domain.World;
import com.lwe.core.repository.WorldEventRepository;
import com.lwe.core.repository.WorldRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventArchiveJobTest {

    @Mock private EntityManager entityManager;
    @Mock private Query insertQuery;
    @Mock private Query deleteQuery;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldEventRepository eventRepo;

    @InjectMocks
    private EventArchiveJob job;

    @Test
    void archiveOldEvents_shouldArchivePerWorld() {
        var worldId = UUID.randomUUID();
        var world = new World("Test", UUID.randomUUID(), null, "{}");
        setId(world, worldId);
        when(worldRepo.findByActiveTrue()).thenReturn(List.of(world));
        when(entityManager.createNativeQuery(anyString()))
            .thenReturn(insertQuery, deleteQuery);
        when(insertQuery.setParameter(anyInt(), any())).thenReturn(insertQuery);
        when(deleteQuery.setParameter(anyInt(), any())).thenReturn(deleteQuery);
        when(insertQuery.executeUpdate()).thenReturn(3);

        job.archiveOldEvents();

        verify(worldRepo).findByActiveTrue();
        verify(entityManager, times(2)).createNativeQuery(anyString());
        verify(insertQuery).setParameter(eq(1), eq(worldId));
        verify(deleteQuery).setParameter(eq(1), eq(worldId));
        verify(insertQuery).executeUpdate();
        verify(deleteQuery).executeUpdate();
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
