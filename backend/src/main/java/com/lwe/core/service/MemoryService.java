package com.lwe.core.service;

import com.lwe.core.domain.EntityMemory;
import com.lwe.core.repository.EntityMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MemoryService {

    private final EntityMemoryRepository repo;

    public MemoryService(EntityMemoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public EntityMemory addMemory(UUID entityId, UUID subjectId, String memoryType,
                                   int sentiment, String summary, Long sourceEventId) {
        var mem = new EntityMemory(entityId, subjectId, memoryType, sentiment, summary);
        if (sourceEventId != null) mem.setSourceEventId(sourceEventId);
        return repo.save(mem);
    }

    public List<EntityMemory> getMemories(UUID entityId) {
        return repo.findByEntityIdOrderByCreatedAtDesc(entityId);
    }

    public List<EntityMemory> getMemoriesAbout(UUID subjectId) {
        return repo.findBySubjectIdOrderByCreatedAtDesc(subjectId);
    }
}
