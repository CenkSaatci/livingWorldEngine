package com.lwe.integration;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.repository.*;
import com.lwe.core.service.RollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🟢 Phase 2 Meilenstein M2: Integrationstest für den vollständigen Flow.
 * Game-System hochladen → Welt erstellen → Charakter anlegen → Probe würfeln.
 */
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RuleEngineFlowTest {

    @Autowired private UserRepository userRepo;
    @Autowired private GameSystemRepository gameSystemRepo;
    @Autowired private WorldRepository worldRepo;
    @Autowired private GameEntityRepository entityRepo;
    @Autowired private RollService rollService;

    private static final String D20_RULES = """
        {"version":1,"attributes":[{"name":"staerke","type":"INT","min":1,"max":20,"default":10}],"dice_mechanics":{"probe":"1d20+mod"}}
        """;

    @Test
    void fullRuleEngineFlow() {
        var tag = UUID.randomUUID().toString().substring(0, 8);

        var user = userRepo.save(new User("flow+" + tag + "@test.de", "flow_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var schema = "{\"type\":\"object\",\"properties\":{}}";
        var gs = gameSystemRepo.save(new GameSystem("FlowD20_" + tag, 1, D20_RULES, schema));
        var world = worldRepo.save(new World("FlowWorld_" + tag, user.getId(), gs.getId(),
            "{\"ai_mode\":\"suggest\"}"));
        var entity = entityRepo.save(new GameEntity(world.getId(), "PC", "FlowHero_" + tag));
        entity.setAttributesJson("{\"staerke\":16,\"geschicklichkeit\":14}");
        entity = entityRepo.save(entity);

        var result = rollService.executeRoll(user.getId(), world.getId(), entity.getId(), "staerke", 0, 12);

        assertThat(result).isNotNull();
        assertThat(result.error()).isNull();
        assertThat(result.skillId()).isEqualTo("staerke");
        assertThat(result.dice()).hasSize(1);
        assertThat(result.total()).isBetween(4, 23);
        assertThat(result.expression()).contains("1d20");
    }

    @Test
    void rollFailsWithWrongUser() {
        var wrongId = UUID.randomUUID();
        var result = rollService.executeRoll(wrongId, wrongId, wrongId, "staerke", 0, 10);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotNull();
    }
}