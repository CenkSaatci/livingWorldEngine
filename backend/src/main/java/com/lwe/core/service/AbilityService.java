package com.lwe.core.service;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.repository.AbilityRepository;
import com.lwe.core.repository.GameSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AbilityService {

    private final AbilityRepository repo;
    private final GameSystemRepository systemRepo;
    private final GameSystemService gameSystemService;

    public AbilityService(AbilityRepository repo, GameSystemRepository systemRepo,
                          GameSystemService gameSystemService) {
        this.repo = repo;
        this.systemRepo = systemRepo;
        this.gameSystemService = gameSystemService;
    }

    @Transactional
    public Ability create(UUID gameSystemId, UUID userId, boolean isAdmin, String name, AbilityType type,
                          String description, String effectsJson, String statBonusesJson,
                          int apCost, int cooldownRounds, String targetType) {
        requireSystem(gameSystemId);
        gameSystemService.requireOwnerForSystem(gameSystemId, userId, isAdmin);

        var ability = new Ability(gameSystemId, name, type);
        if (description != null) ability.setDescription(description);
        if (effectsJson != null) ability.setEffectsJson(effectsJson);
        if (statBonusesJson != null) ability.setStatBonusesJson(statBonusesJson);
        ability.setApCost(apCost);
        ability.setCooldownRounds(cooldownRounds);
        if (targetType != null) ability.setTargetType(targetType);

        return repo.save(ability);
    }

    public List<Ability> listByGameSystem(UUID gameSystemId, UUID userId, boolean isAdmin) {
        requireSystem(gameSystemId);
        gameSystemService.requireReadableForSystem(gameSystemId, userId, isAdmin);
        return repo.findByGameSystemIdOrderByNameAsc(gameSystemId);
    }

    public Ability getById(UUID id, UUID userId, boolean isAdmin) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        gameSystemService.requireReadableForSystem(ability.getGameSystemId(), userId, isAdmin);
        return ability;
    }

    @Transactional
    public Ability update(UUID id, UUID userId, boolean isAdmin, String name, String description,
                          String effectsJson, String statBonusesJson,
                          Integer apCost, Integer cooldownRounds, String targetType) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        requireSystem(ability.getGameSystemId());
        gameSystemService.requireOwnerForSystem(ability.getGameSystemId(), userId, isAdmin);

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
    public void delete(UUID id, UUID userId, boolean isAdmin) {
        var ability = repo.findById(id)
            .orElseThrow(() -> new AbilityException("ABILITY_NOT_FOUND", "Ability not found"));
        requireSystem(ability.getGameSystemId());
        gameSystemService.requireOwnerForSystem(ability.getGameSystemId(), userId, isAdmin);
        repo.delete(ability);
    }

    private void requireSystem(UUID gameSystemId) {
        systemRepo.findById(gameSystemId)
            .orElseThrow(() -> new AbilityException("GAME_SYSTEM_NOT_FOUND", "Game system not found"));
    }

    public static class AbilityException extends RuntimeException {
        private final String errorCode;
        public AbilityException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
