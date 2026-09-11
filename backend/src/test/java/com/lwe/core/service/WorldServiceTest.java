package com.lwe.core.service;

import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.domain.WorldMember;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.FactionRepository;
import com.lwe.core.repository.FactionRelationRepository;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.RegionWeatherRepository;
import com.lwe.core.repository.EntityAbilityRepository;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Region;
import com.lwe.core.domain.Location;
import com.lwe.core.domain.RegionWeather;
import com.lwe.core.domain.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldServiceTest {

    @Mock private WorldRepository worldRepo;
    @Mock private WorldMemberRepository memberRepo;
    @Mock private QuotaService quotaService;
    @Mock private RegionRepository regionRepo;
    @Mock private LocationRepository locationRepo;
    @Mock private GameEntityRepository entityRepo;
    @Mock private FactionRepository factionRepo;
    @Mock private FactionRelationRepository factionRelationRepo;
    @Mock private WorldMapRepository worldMapRepo;
    @Mock private RegionWeatherRepository regionWeatherRepo;
    @Mock private EntityAbilityRepository entityAbilityRepo;
    @Mock private com.lwe.core.util.WorldAccess worldAccess;

    private WorldService worldService;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        worldService = new WorldService(worldRepo, memberRepo, quotaService,
            regionRepo, locationRepo, entityRepo, factionRepo, factionRelationRepo,
            worldMapRepo, regionWeatherRepo, entityAbilityRepo, worldAccess);
    }

    @Test
    void shouldCreateWorld() {
        var user = new User("test@test.com", "test", "hash", "USER", "de");
        doNothing().when(quotaService).checkCanCreateWorld(ownerId, user);
        when(worldRepo.save(any())).thenAnswer(inv -> {
            var w = inv.<World>getArgument(0);
            setId(w, UUID.randomUUID());
            return w;
        });

        var result = worldService.create("Schattental", ownerId, "{}", user);

        assertThat(result.getName()).isEqualTo("Schattental");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldListOwnedWorlds() {
        when(worldRepo.findByOwnerIdAndActiveTrue(ownerId)).thenReturn(List.of());
        assertThat(worldService.listOwned(ownerId)).isEmpty();
    }

    @Test
    void shouldListAccessibleIncludingMemberWorlds() {
        var ownedWorld = worldWithId("Owned", ownerId);
        var memberWorld = worldWithId("Joined", ownerId); // owned by SOMEONE else

        when(memberRepo.findWorldIdsByUserId(memberId)).thenReturn(List.of(memberWorld.getId()));
        when(worldRepo.findByOwnerIdAndActiveTrue(memberId)).thenReturn(List.of());
        when(worldRepo.findAllById(List.of(memberWorld.getId()))).thenReturn(List.of(memberWorld));

        var list = worldService.listAccessible(memberId);
        assertThat(list).hasSize(1);
    }

    @Test
    void shouldAllowOwnerAccess() {
        var world = worldWithId("Test", ownerId);
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));

        var result = worldService.getById(world.getId(), ownerId);
        assertThat(result.getName()).isEqualTo("Test");
    }

    @Test
    void shouldAllowMemberAccess() {
        var world = worldWithId("Test", UUID.randomUUID());
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));

        var result = worldService.getById(world.getId(), memberId);
        assertThat(result.getName()).isEqualTo("Test");
    }

    @Test
    void shouldRejectAccessToStranger() {
        var world = worldWithId("Secret", UUID.randomUUID());
        doThrow(new com.lwe.core.util.WorldAccess.WorldAccessException(
                "WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireRead(world.getId(), ownerId);

        assertThatThrownBy(() -> worldService.getById(world.getId(), ownerId))
            .isInstanceOf(com.lwe.core.util.WorldAccess.WorldAccessException.class);
    }

    @Test
    void shouldSoftDeleteWorld() {
        var world = worldWithId("Test", ownerId);
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));

        worldService.delete(world.getId(), ownerId);
        assertThat(world.isActive()).isFalse();
        verify(worldRepo).save(world);
    }

    @Test
    void shouldRejectDeleteByNonOwner() {
        var world = worldWithId("Test", UUID.randomUUID());
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));

        assertThatThrownBy(() -> worldService.delete(world.getId(), ownerId))
            .isInstanceOf(WorldService.WorldException.class)
            .matches(e -> ((WorldService.WorldException) e).getErrorCode().equals("WORLD_OWNER_REQUIRED"));
    }

    @Test
    void shouldAddMember() {
        var world = worldWithId("Test", ownerId);
        doNothing().when(quotaService).checkCanAddMember(world.getId(), ownerId);
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));
        when(memberRepo.existsByWorldIdAndUserId(world.getId(), memberId)).thenReturn(false);
        when(memberRepo.save(any())).thenAnswer(inv -> {
            var m = inv.<WorldMember>getArgument(0);
            var f = WorldMember.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, UUID.randomUUID());
            return m;
        });

        var result = worldService.addMember(world.getId(), ownerId, memberId, "PLAYER");
        assertThat(result.getUserId()).isEqualTo(memberId);
        assertThat(result.getRole()).isEqualTo("PLAYER");
    }

    @Test
    void shouldRejectAddDuplicateMember() {
        var world = worldWithId("Test", ownerId);
        doNothing().when(quotaService).checkCanAddMember(world.getId(), ownerId);
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));
        when(memberRepo.existsByWorldIdAndUserId(world.getId(), memberId)).thenReturn(true);

        assertThatThrownBy(() -> worldService.addMember(world.getId(), ownerId, memberId, "PLAYER"))
            .isInstanceOf(WorldService.WorldException.class)
            .matches(e -> ((WorldService.WorldException) e).getErrorCode().equals("WORLD_MEMBER_ALREADY"));
    }

    @Test
    void ownerCanSetVisibilityAndInvalidVisibilityIsRejected() {
        var world = worldWithId("W", ownerId);
        setId(world, UUID.randomUUID());
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));
        when(worldRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = worldService.update(world.getId(), ownerId, null, null, "PUBLIC");
        assertThat(updated.getVisibility()).isEqualTo("PUBLIC");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> worldService.update(world.getId(), ownerId, null, null, "CHAOS"))
            .isInstanceOf(WorldService.WorldException.class)
            .matches(e -> ((WorldService.WorldException) e).getErrorCode().equals("INVALID_VISIBILITY"));
    }

    @Test
    void shouldCloneWorld() {
        var user = new User("test@test.com", "test", "hash", "USER", "de");
        var original = worldWithId("Schattental", ownerId);
        setId(original, UUID.randomUUID());

        when(worldRepo.findById(original.getId())).thenReturn(Optional.of(original));
        when(worldRepo.save(any())).thenAnswer(inv -> {
            var w = inv.<World>getArgument(0);
            setId(w, UUID.randomUUID());
            return w;
        });

        var clone = worldService.clone(original.getId(), ownerId, user);

        assertThat(clone).isNotNull();
        assertThat(clone.getName()).startsWith("Schattental");
        assertThat(clone.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void cloneForCampaignSkipsQuotaAndCopiesRuntimeFields() {
        var original = worldWithId("Schattental", ownerId);
        setId(original, UUID.randomUUID());
        var npc = new GameEntity(original.getId(), "PC", "Held");
        npc.setSkillsJson("{\"Klettern\":12}");
        npc.setExperiencePoints(250);
        npc.setHpCurrent(7);
        npc.setHpMax(11);
        setId(npc, UUID.randomUUID());

        when(worldRepo.findById(original.getId())).thenReturn(Optional.of(original));
        when(worldRepo.save(any())).thenAnswer(inv -> {
            var w = inv.<World>getArgument(0);
            if (w.getId() == null) setId(w, UUID.randomUUID());
            return w;
        });
        when(entityRepo.findByWorldIdAndActiveTrue(original.getId())).thenReturn(List.of(npc));
        when(entityRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<GameEntity>getArgument(0);
            if (e.getId() == null) setId(e, UUID.randomUUID());
            return e;
        });

        var fork = worldService.cloneForCampaign(original.getId(), memberId);

        assertThat(fork.getName()).contains("(Kampagne)");
        assertThat(fork.getOwnerId()).isEqualTo(memberId);
        verify(quotaService, never()).checkCanCreateWorld(any(), any());
        var captor = org.mockito.ArgumentCaptor.forClass(GameEntity.class);
        verify(entityRepo, atLeastOnce()).save(captor.capture());
        var copied = captor.getAllValues().stream()
            .filter(e2 -> "Held".equals(e2.getName())).findFirst().orElseThrow();
        assertThat(copied.getSkillsJson()).isEqualTo("{\"Klettern\":12}");
        assertThat(copied.getExperiencePoints()).isEqualTo(250);
        assertThat(copied.getHpCurrent()).isEqualTo(7);
        assertThat(copied.getHpMax()).isEqualTo(11);
    }

    @Test
    void cloneCopiesCapitalWeatherDescriptionAndLeader() {
        var original = worldWithId("Detailwelt", ownerId);
        setId(original, UUID.randomUUID());

        var region = new Region(original.getId(), "Nord");
        setId(region, UUID.randomUUID());
        var loc = new Location(region.getId(), "Stadt", "Hauptstadt");
        loc.setCapital(true);
        setId(loc, UUID.randomUUID());
        var weather = new RegionWeather(region.getId());
        weather.setDescription("Regnerisch");
        var leader = new GameEntity(original.getId(), "NPC", "Baron");
        setId(leader, UUID.randomUUID());
        var faction = new Faction(original.getId(), "Barons");
        faction.setLeaderEntityId(leader.getId());
        setId(faction, UUID.randomUUID());

        when(worldRepo.findById(original.getId())).thenReturn(Optional.of(original));
        when(worldRepo.save(any())).thenAnswer(inv -> {
            var w = inv.<World>getArgument(0);
            if (w.getId() == null) setId(w, UUID.randomUUID());
            return w;
        });
        when(regionRepo.findByWorldIdOrderByNameAsc(original.getId())).thenReturn(List.of(region));
        when(regionRepo.save(any())).thenAnswer(inv -> {
            var r = inv.<Region>getArgument(0);
            setId(r, UUID.randomUUID());
            return r;
        });
        when(locationRepo.findByRegionIdIn(any())).thenReturn(List.of(loc));
        when(locationRepo.save(any())).thenAnswer(inv -> {
            var l = inv.<Location>getArgument(0);
            setId(l, UUID.randomUUID());
            return l;
        });
        when(regionWeatherRepo.findByRegionIdIn(any())).thenReturn(List.of(weather));
        when(factionRepo.findByWorldIdOrderByNameAsc(original.getId())).thenReturn(List.of(faction));
        when(factionRepo.save(any())).thenAnswer(inv -> {
            var f = inv.<Faction>getArgument(0);
            if (f.getId() == null) setId(f, UUID.randomUUID());
            return f;
        });
        when(entityRepo.findByWorldIdAndActiveTrue(original.getId())).thenReturn(List.of(leader));
        when(entityRepo.save(any())).thenAnswer(inv -> {
            var e = inv.<GameEntity>getArgument(0);
            if (e.getId() == null) setId(e, UUID.randomUUID());
            return e;
        });
        when(factionRepo.findById(any())).thenAnswer(inv -> {
            var f = new Faction(UUID.randomUUID(), "Barons");
            setId(f, inv.getArgument(0));
            return Optional.of(f);
        });

        worldService.cloneForCampaign(original.getId(), memberId);

        var locCaptor = org.mockito.ArgumentCaptor.forClass(Location.class);
        verify(locationRepo, atLeastOnce()).save(locCaptor.capture());
        assertThat(locCaptor.getAllValues().stream().anyMatch(Location::isCapital))
            .as("Hauptstadt-Flag muss mitkopiert werden").isTrue();

        var weatherCaptor = org.mockito.ArgumentCaptor.forClass(RegionWeather.class);
        verify(regionWeatherRepo).save(weatherCaptor.capture());
        assertThat(weatherCaptor.getValue().getDescription()).isEqualTo("Regnerisch");

        var factionCaptor = org.mockito.ArgumentCaptor.forClass(Faction.class);
        verify(factionRepo, atLeastOnce()).save(factionCaptor.capture());
    }

    // -- helpers --

    private World worldWithId(String name, UUID owner) {
        var w = new World(name, owner, "{}");
        setId(w, UUID.randomUUID());
        return w;
    }

    private void setId(Object obj, UUID id) {
        try {
            var f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}