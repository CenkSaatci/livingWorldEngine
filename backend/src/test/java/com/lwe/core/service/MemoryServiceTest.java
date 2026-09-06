package com.lwe.core.service;

import com.lwe.core.domain.EntityMemory;
import com.lwe.core.repository.EntityMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock private EntityMemoryRepository repo;

    @InjectMocks private MemoryService memoryService;

    @Test
    void shouldAddMemory() {
        var entityId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();

        when(repo.save(any())).thenAnswer(inv -> inv.<EntityMemory>getArgument(0));

        var result = memoryService.addMemory(entityId, subjectId, "ENCOUNTER", 5, "met a dragon", 42L);

        assertThat(result.getEntityId()).isEqualTo(entityId);
        assertThat(result.getSubjectId()).isEqualTo(subjectId);
        assertThat(result.getMemoryType()).isEqualTo("ENCOUNTER");
        assertThat(result.getSentiment()).isEqualTo(5);
        assertThat(result.getSummary()).isEqualTo("met a dragon");
        assertThat(result.getSourceEventId()).isEqualTo(42L);
        verify(repo).save(any());
    }

    @Test
    void shouldGetMemoriesByEntityId() {
        var entityId = UUID.randomUUID();
        var mem = new EntityMemory(entityId, UUID.randomUUID(), "ENCOUNTER", 3, "saw a goblin");
        when(repo.findByEntityIdOrderByCreatedAtDesc(entityId)).thenReturn(List.of(mem));

        var result = memoryService.getMemories(entityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemoryType()).isEqualTo("ENCOUNTER");
    }

    @Test
    void shouldGetMemoriesAboutSubject() {
        var subjectId = UUID.randomUUID();
        var mem = new EntityMemory(UUID.randomUUID(), subjectId, "ENCOUNTER", -2, "bad day");
        when(repo.findBySubjectIdOrderByCreatedAtDesc(subjectId)).thenReturn(List.of(mem));

        var result = memoryService.getMemoriesAbout(subjectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummary()).isEqualTo("bad day");
    }
}
