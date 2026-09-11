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

    private WorldService worldService;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        worldService = new WorldService(worldRepo, memberRepo, quotaService,
            regionRepo, locationRepo, entityRepo, factionRepo, factionRelationRepo,
            worldMapRepo, regionWeatherRepo);
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
        var world = worldWithId("Test", UUID.randomUUID()); // owned by someone else
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));
        when(memberRepo.existsByWorldIdAndUserId(world.getId(), memberId)).thenReturn(true);

        var result = worldService.getById(world.getId(), memberId);
        assertThat(result.getName()).isEqualTo("Test");
    }

    @Test
    void shouldRejectAccessToStranger() {
        var world = worldWithId("Secret", UUID.randomUUID());
        when(worldRepo.findById(world.getId())).thenReturn(Optional.of(world));
        when(memberRepo.existsByWorldIdAndUserId(world.getId(), ownerId)).thenReturn(false);

        assertThatThrownBy(() -> worldService.getById(world.getId(), ownerId))
            .isInstanceOf(WorldService.WorldException.class)
            .matches(e -> ((WorldService.WorldException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
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

    // -- helpers --

    private World worldWithId(String name, UUID owner) {
        var w = new World(name, owner, "{}");
        setId(w, UUID.randomUUID());
        return w;
    }

    private void setId(World w, UUID id) {
        try {
            var f = World.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(w, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}