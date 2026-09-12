package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.domain.EntityAbility;
import com.lwe.core.repository.AbilityRepository;
import com.lwe.core.repository.EntityAbilityRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EntityAbilityService {

    private final EntityAbilityRepository repo;
    private final GameEntityRepository entityRepo;
    private final AbilityRepository abilityRepo;
    private final WorldAccess worldAccess;
    private final EntityAccess entityAccess;

    public EntityAbilityService(EntityAbilityRepository repo,
                                GameEntityRepository entityRepo,
                                AbilityRepository abilityRepo,
                                WorldAccess worldAccess, EntityAccess entityAccess) {
        this.repo = repo;
        this.entityRepo = entityRepo;
        this.abilityRepo = abilityRepo;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
    }

    @Transactional
    public EntityAbility assign(UUID entityId, UUID abilityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityAbilityException("ENTITY_NOT_FOUND", "Entity not found"));
        entityAccess.checkControl(entity, userId); // Runde 1
        var ability = abilityRepo.findById(abilityId)
            .orElseThrow(() -> new EntityAbilityException("ABILITY_NOT_FOUND", "Ability not found"));

        if (repo.findByEntityIdAndAbilityId(entityId, abilityId).isPresent())
            throw new EntityAbilityException("ALREADY_ASSIGNED", "Ability already assigned to entity");

        return repo.save(new EntityAbility(entityId, abilityId));
    }

    public List<EntityAbility> listByEntity(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityAbilityException("ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
        return repo.findByEntityId(entityId);
    }

    public Ability getAbility(UUID abilityId) {
        return abilityRepo.findById(abilityId)
            .orElseThrow(() -> new EntityAbilityException("ABILITY_NOT_FOUND", "Ability not found"));
    }

    @Transactional
    public void unassign(UUID entityId, UUID abilityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityAbilityException("ENTITY_NOT_FOUND", "Entity not found"));
        entityAccess.checkControl(entity, userId); // Runde 1
        var ea = repo.findByEntityIdAndAbilityId(entityId, abilityId)
            .orElseThrow(() -> new EntityAbilityException("NOT_ASSIGNED", "Ability not assigned to entity"));
        repo.delete(ea);
    }

    public Map<String, Integer> calculatePassiveBonuses(UUID entityId) {
        var assigned = repo.findByEntityId(entityId);
        var bonuses = new java.util.HashMap<String, Integer>();
        var mapper = new ObjectMapper();

        for (var ea : assigned) {
            var ability = abilityRepo.findById(ea.getAbilityId()).orElse(null);
            if (ability == null || ability.getType() != AbilityType.PASSIVE) continue;
            try {
                var tree = mapper.readTree(ability.getStatBonusesJson());
                var it = tree.fields();
                while (it.hasNext()) {
                    var entry = it.next();
                    bonuses.merge(entry.getKey(), entry.getValue().asInt(0), Integer::sum);
                }
            } catch (Exception ignored) {}
        }

        return bonuses;
    }

    public static class EntityAbilityException extends RuntimeException {
        private final String errorCode;
        public EntityAbilityException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
