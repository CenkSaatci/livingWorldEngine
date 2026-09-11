package com.lwe.integration;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Location;
import com.lwe.core.domain.Quest;
import com.lwe.core.domain.Region;
import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.QuestRepository;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.EntityService;
import com.lwe.core.service.LocationService;
import com.lwe.core.service.QuestService;
import com.lwe.core.service.RegionService;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F1-Audit Phase 33: PUBLIC-Welten duerfen nur LESBAR sein — Schreibpfade muessen
 * fuer Fremde 403 werfen (echter WorldAccess statt Mock).
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PublicWorldWriteAccessIT {

    @Autowired private UserRepository userRepo;
    @Autowired private WorldRepository worldRepo;
    @Autowired private RegionRepository regionRepo;
    @Autowired private LocationRepository locationRepo;
    @Autowired private GameEntityRepository entityRepo;
    @Autowired private QuestRepository questRepo;
    @Autowired private EntityService entityService;
    @Autowired private RegionService regionService;
    @Autowired private LocationService locationService;
    @Autowired private QuestService questService;

    @Test
    void strangerCanReadButNotWritePublicWorldContent() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var owner = userRepo.save(new User("pub-owner+" + tag + "@test.de", "po_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var stranger = userRepo.save(new User("pub-stranger+" + tag + "@test.de", "ps_" + tag,
            "$2a$10$dummyhash", "USER", "de"));

        var world = worldRepo.save(new World("PUB_W_" + tag, owner.getId(), "{}"));
        world.setVisibility("PUBLIC");
        world = worldRepo.save(world);

        var region = regionRepo.save(new Region(world.getId(), "Nord"));
        var location = locationRepo.save(new Location(region.getId(), "Dorf", "Start"));
        var entity = entityRepo.save(new GameEntity(world.getId(), "PC", "Held"));
        var quest = questRepo.save(new Quest(world.getId(), "Banditen", "kill", "[]", "{}"));

        // LESEN ok (PUBLIC)
        assertThatCode(() -> entityService.getById(entity.getId(), stranger.getId()))
            .doesNotThrowAnyException();
        assertThatCode(() -> regionService.getById(region.getId(), stranger.getId()))
            .doesNotThrowAnyException();
        assertThatCode(() -> locationService.getById(location.getId(), stranger.getId()))
            .doesNotThrowAnyException();
        assertThatCode(() -> questService.getById(quest.getId(), stranger.getId()))
            .doesNotThrowAnyException();

        // SCHREIBEN verboten
        assertThatThrownBy(() -> entityService.updateAttributes(
                entity.getId(), stranger.getId(), Map.of("staerke", 99)))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        assertThatThrownBy(() -> entityService.updateOverrides(
                entity.getId(), stranger.getId(), Map.of("hp", 1)))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        assertThatThrownBy(() -> regionService.update(region.getId(), stranger.getId(),
                "HACKED", null, null, null, null, null, null, null, null, null))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        assertThatThrownBy(() -> locationService.update(location.getId(), stranger.getId(),
                null, "HACKED", null, null, null, null, null, null, null, null))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        assertThatThrownBy(() -> questService.updateStatus(quest.getId(), stranger.getId(), "done"))
            .isInstanceOf(WorldAccess.WorldAccessException.class);

        // Owner darf schreiben
        assertThatCode(() -> entityService.updateAttributes(
                entity.getId(), owner.getId(), Map.of("staerke", 15)))
            .doesNotThrowAnyException();
    }
}
