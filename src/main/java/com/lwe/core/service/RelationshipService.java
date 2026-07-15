package com.lwe.core.service;

import com.lwe.core.domain.EntityRelationship;
import com.lwe.core.repository.EntityRelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RelationshipService {

    private final EntityRelationshipRepository repo;

    public RelationshipService(EntityRelationshipRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public EntityRelationship setRelationship(UUID entityAId, UUID entityBId, String relationship) {
        var existing = repo.findByEntityAIdAndEntityBId(entityAId, entityBId)
            .orElseGet(() -> repo.findByEntityAIdAndEntityBId(entityBId, entityAId)
                .orElse(null));
        if (existing != null) {
            existing.setRelationship(relationship);
            return repo.save(existing);
        }
        return repo.save(new EntityRelationship(entityAId, entityBId, relationship));
    }

    public List<EntityRelationship> getRelationships(UUID entityId) {
        return repo.findByEntityAIdOrEntityBId(entityId, entityId);
    }
}
