package com.lwe.integration;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.repository.*;
import com.lwe.core.service.CharacterSheetService;
import com.lwe.core.service.CampaignService;
import com.lwe.core.service.ProbeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testet den Per-Character Skill-Override Mechanismus system-agnostisch.
 * Das Regelwerk definiert nur einen Skill mit globalem Bonus 0.
 * Per-Character-Werte überschreiben diesen globalen Bonus.
 */
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PerCharacterSkillFlowTest {

    @Autowired private UserRepository userRepo;
    @Autowired private GameSystemRepository gameSystemRepo;
    @Autowired private WorldRepository worldRepo;
    @Autowired private GameEntityRepository entityRepo;
    @Autowired private ProbeService probeService;
    @Autowired private CampaignService campaignService;
    @Autowired private CharacterSheetService sheetService;

    private static final String GENERIC_RULES = """
        {"version":1,"probeType":"d20_target",
         "attributes":[{"name":"staerke","type":"INT","min":1,"max":20,"default":10}],
         "skills":[{"name":"Athletik","attributes":["staerke"],"bonus":0}],
         "dice_mechanics":{"probe":"1d20+mod"}}
        """;

    @Test
    void perCharacterSkillOverridesGlobalBonus() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var user = userRepo.save(new User("pc+" + tag + "@test.de", "pc_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var schema = "{\"type\":\"object\",\"properties\":{}}";
        var gs = gameSystemRepo.save(new GameSystem("PCTest_" + tag, 1, GENERIC_RULES, schema));
        var world = worldRepo.save(new World("PCWorld_" + tag, user.getId(), "{}"));
        var campaign = campaignService.create(world.getId(), gs.getId(), "Camp_" + tag, user.getId());
        var entity = entityRepo.save(new GameEntity(campaign.getWorldId(), "PC", "PCHero_" + tag));
        entity.setAttributesJson("{\"staerke\":10}");
        entity.setSkillsJson("{\"Athletik\":5}");
        entity = entityRepo.save(entity);

        var result = probeService.executeProbe(entity.getId(), user.getId(), "Athletik", 15, false, campaign.getId());

        // Per-Character Bonus = 5, kein modifierFormula → attrMod = 0
        // total = die(1-20) + 5, range 6-25
        assertThat(result.modifier()).isEqualTo(5);
        assertThat(result.total()).isGreaterThanOrEqualTo(6);
        assertThat(result.probeType()).isEqualTo("d20_target");
    }

    @Test
    void perCharacterSkillFallbackToGlobal() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var user = userRepo.save(new User("fb+" + tag + "@test.de", "fb_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var schema = "{\"type\":\"object\",\"properties\":{}}";
        var gs = gameSystemRepo.save(new GameSystem("FbTest_" + tag, 1, GENERIC_RULES, schema));
        var world = worldRepo.save(new World("FbWorld_" + tag, user.getId(), "{}"));
        var campaign = campaignService.create(world.getId(), gs.getId(), "Camp_" + tag, user.getId());
        var entity = entityRepo.save(new GameEntity(campaign.getWorldId(), "PC", "FbHero_" + tag));
        entity.setAttributesJson("{\"staerke\":10}");
        entity = entityRepo.save(entity);

        var result = probeService.executeProbe(entity.getId(), user.getId(), "Athletik", 15, false, campaign.getId());

        // Kein per-Character → globaler Bonus 0, attrMod = 0
        // total = die(1-20), range 1-20
        assertThat(result.modifier()).isEqualTo(0);
        assertThat(result.total()).isBetween(1, 20);
    }

    @Test
    void sheetResponseIncludesPerCharacterValue() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var user = userRepo.save(new User("sh+" + tag + "@test.de", "sh_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var schema = "{\"type\":\"object\",\"properties\":{}}";
        var gs = gameSystemRepo.save(new GameSystem("ShTest_" + tag, 1, GENERIC_RULES, schema));
        var world = worldRepo.save(new World("ShWorld_" + tag, user.getId(), "{}"));
        var campaign = campaignService.create(world.getId(), gs.getId(), "Camp_" + tag, user.getId());
        var entity = entityRepo.save(new GameEntity(campaign.getWorldId(), "PC", "ShHero_" + tag));
        entity.setAttributesJson("{\"staerke\":10}");
        entity.setSkillsJson("{\"Athletik\":7}");
        entity = entityRepo.save(entity);

        var sheet = sheetService.getSheet(entity.getId(), user.getId(), campaign.getId());

        var athletik = sheet.skills().stream()
            .filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertThat(athletik.perCharacterValue()).isEqualTo(7);
        // total = perCharacter(7) + attrMod(0) = 7
        assertThat(athletik.total()).isEqualTo(7);
    }

    @Test
    void sheetResponseFallbackWhenNoSkillsJson() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var user = userRepo.save(new User("sf+" + tag + "@test.de", "sf_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var schema = "{\"type\":\"object\",\"properties\":{}}";
        var gs = gameSystemRepo.save(new GameSystem("SfTest_" + tag, 1, GENERIC_RULES, schema));
        var world = worldRepo.save(new World("SfWorld_" + tag, user.getId(), "{}"));
        var campaign = campaignService.create(world.getId(), gs.getId(), "Camp_" + tag, user.getId());
        var entity = entityRepo.save(new GameEntity(campaign.getWorldId(), "PC", "SfHero_" + tag));
        entity.setAttributesJson("{\"staerke\":10}");
        entity = entityRepo.save(entity);

        var sheet = sheetService.getSheet(entity.getId(), user.getId(), campaign.getId());

        var athletik = sheet.skills().stream()
            .filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertThat(athletik.perCharacterValue()).isNull();
        assertThat(athletik.total()).isEqualTo(0); // global bonus 0 + attrMod 0
    }
}
