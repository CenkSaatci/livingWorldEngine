package com.lwe.events;

import com.lwe.core.domain.EntityMemory;
import com.lwe.core.repository.EntityMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryCleanupJobTest {

    @Mock
    private EntityMemoryRepository repo;

    @InjectMocks
    private MemoryCleanupJob job;

    @Test
    void decayMemories_shouldDeleteOldZeroSentimentMemories() {
        var oldZero = new EntityMemory(null, null, "test", 0, "old zero");
        when(repo.findByCreatedAtBeforeAndSentiment(any(Instant.class), eq(0)))
            .thenReturn(List.of(oldZero));

        job.decayMemories();

        verify(repo).deleteAll(List.of(oldZero));
    }

    @Test
    void decayMemories_shouldDriftSentimentTowardZero() {
        var positive = new EntityMemory(null, null, "test", 5, "positive");
        var negative = new EntityMemory(null, null, "test", -3, "negative");
        var alreadyZero = new EntityMemory(null, null, "test", 0, "zero");

        when(repo.findByCreatedAtBeforeAndSentiment(any(Instant.class), eq(0)))
            .thenReturn(List.of());
        when(repo.findByCreatedAtBefore(any(Instant.class)))
            .thenReturn(List.of(positive, negative, alreadyZero));

        job.decayMemories();

        verify(repo).saveAll(List.of(positive, negative, alreadyZero));
        assertThat(positive.getSentiment()).isEqualTo(4);
        assertThat(negative.getSentiment()).isEqualTo(-2);
        assertThat(alreadyZero.getSentiment()).isEqualTo(0);
    }
}
