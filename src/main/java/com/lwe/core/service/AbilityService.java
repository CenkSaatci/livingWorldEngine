package com.lwe.core.service;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.repository.AbilityRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AbilityService {

    private final AbilityRepository repo;
    private final WorldRepository worldRepo;

    public AbilityService(AbilityRepository repo, WorldRepository worldRepo) {
        this.repo = repo;
        this.worldRepo = worldRepo;
    }

    @Transactional
    public Ability create(UUID worldId, UUID userId, String name, AbilityType type,
                          String description, String effectsJson, String statBonusesJson,
                          int apCost, int cooldownRounds, String targetType) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new AbilityException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new AbilityException("WORLD_ACCESS_DENIED", "Only the owner may create abilities");

        var ability = new Ability(worldId, name, type);
        if (description != null) ability.setDescription(description);
        if (effectsJson != null) ability.setEffectsJson(effectsJson);
        if (statBonusesJson != null) ability.setStatBonusesJson(statBonusesJson);
        ability.setApCost(apCost);
        ability.setCooldownRounds(cooldownRounds);
        if (targetType != null) ability.setTargetType(targetType);

        return repo.save(ability);
    }

    public List<Ability> listByWorld(UUID worldId, UUID userId) {
        worldRepo.findById(worldId)
            .orElseThrow(() -> new AbilityException("WORLD_NOT_FOUND", "World not found"));
        return repo.findByWorldIdOrderByNameAsc(worldId);
    }

    public Ability getById(UUID id, UUID userId) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        return ability;
    }

    @Transactional
    public Ability update(UUID id, UUID userId, String name, String description,
                          String effectsJson, String statBonusesJson,
                          Integer apCost, Integer cooldownRounds, String targetType) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        requireOwner(ability.getWorldId(), userId);

        if (name != null) ability.setName(name);
        if (description != null) ability.setDescription(description);
        if (effectsJson != null) ability.setEffectsJson(effectsJson);
        if (statBonusesJson != null) ability.setStatBonusesJson(statBonusesJson);
        if (apCost != null) ability.setApCost(apCost);
        if (cooldownRounds != null) ability.setCooldownRounds(cooldownRounds);
        if (targetType != null) ability.setTargetType(targetType);

        return repo.save(ability);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        requireOwner(ability.getWorldId(), userId);
        repo.delete(ability);
    }

    private void requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new AbilityException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new AbilityException("WORLD_ACCESS_DENIED", "Only the owner may modify abilities");
    }

    public static class AbilityException extends RuntimeException {
        private final String errorCode;
        public AbilityException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
