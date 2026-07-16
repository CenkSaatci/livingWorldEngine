package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.rules.DiceExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RestService {

    private final GameEntityRepository entityRepo;
    private final GameSystemRepository gameSystemRepo;
    private final WorldRepository worldRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public RestService(GameEntityRepository entityRepo, GameSystemRepository gameSystemRepo,
                       WorldRepository worldRepo) {
        this.entityRepo = entityRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.worldRepo = worldRepo;
    }

    @Transactional
    public void rest(UUID entityId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RestException("ENTITY_NOT_FOUND", "Entity not found"));

        var gs = resolveGameSystem(entity.getWorldId());
        var config = parseRestConfig(gs != null ? gs.getRulesJson() : null);

        int healed = 0;
        if ("full".equals(config.hpRecovery())) {
            healed = entity.getHpMax() - entity.getHpCurrent();
            entity.setHpCurrent(entity.getHpMax());
        } else if ("dice".equals(config.hpRecovery()) && config.hpDice() != null) {
            var roll = new DiceExpression(config.hpDice());
            healed = Math.min(roll.getTotal(), entity.getHpMax() - entity.getHpCurrent());
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else if ("percentage".equals(config.hpRecovery())) {
            int pctHeal = (int) Math.round(entity.getHpMax() * config.hpPercentage() / 100.0);
            healed = Math.min(pctHeal, entity.getHpMax() - entity.getHpCurrent());
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else {
            entity.setHpCurrent(entity.getHpMax());
            healed = entity.getHpMax();
        }

        if ("full".equals(config.apRecovery())) {
            entity.setApCurrent(entity.getApMax());
        }

        entityRepo.save(entity);
    }

    private GameSystem resolveGameSystem(UUID worldId) {
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null || world.getGameSystemId() == null) return null;
        return gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
    }

    private RestConfig parseRestConfig(String rulesJson) {
        if (rulesJson == null) return new RestConfig("full", null, 0, "full");
        try {
            var tree = mapper.readTree(rulesJson);
            var rest = tree.path("rest");
            if (rest.isMissingNode()) return new RestConfig("full", null, 0, "full");
            return new RestConfig(
                rest.path("hp_recovery").asText("full"),
                rest.path("hp_dice").asText(null),
                rest.path("hp_percentage").asInt(50),
                rest.path("ap_recovery").asText("full"));
        } catch (Exception e) {
            return new RestConfig("full", null, 0, "full");
        }
    }

    private record RestConfig(String hpRecovery, String hpDice, int hpPercentage, String apRecovery) {}

    public static class RestException extends RuntimeException {
        private final String errorCode;
        public RestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
