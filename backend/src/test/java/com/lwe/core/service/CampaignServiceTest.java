package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.util.WorldAccess.WorldAccessException;
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
class CampaignServiceTest {

    @Mock private CampaignRepository repo;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldMemberRepository memberRepo;
    @Mock private GameSystemRepository systemRepo;
    @Mock private WorldAccess worldAccess;
    @Mock private CampaignMemberService memberService;
    @Mock private WorldService worldService;

    private CampaignService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignService(repo, worldRepo, memberRepo, systemRepo, worldAccess, memberService, worldService, mock(GameSystemService.class));
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
    }

    @Test
    void shouldCreateCampaign() {
        var world = new World("Aventurien", userId, "{}");
        var system = new GameSystem("DSA", 3, "{\"marker\":\"v3\"}", "{}");
        setId(world, worldId);
        setId(system, gameSystemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));
        var fork = new World("Aventurien (Kampagne)", userId, "{}");
        setId(fork, UUID.randomUUID());
        when(worldService.cloneForCampaign(worldId, userId)).thenReturn(fork);
        when(repo.save(any())).thenAnswer(inv -> {
            var c = inv.<Campaign>getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        var campaign = service.create(worldId, gameSystemId, "Runde 1", userId);

        assertThat(campaign.getName()).isEqualTo("Runde 1");
        assertThat(campaign.getRulesJsonSnapshot()).isEqualTo("{\"marker\":\"v3\"}");
        assertThat(campaign.getGameSystemVersion()).isEqualTo(3);
        assertThat(campaign.getWorldId()).isEqualTo(fork.getId());
        assertThat(campaign.isForkedWorld()).isTrue();
        assertThat(campaign.getGameSystemId()).isEqualTo(gameSystemId);
        verify(worldAccess).requireAccess(worldId, userId);
        verify(worldService).cloneForCampaign(worldId, userId);
    }

    @Test
    void shouldRejectUnknownWorld() {
        when(worldRepo.findById(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(worldId, gameSystemId, "X", userId))
            .isInstanceOf(CampaignService.CampaignException.class)
            .matches(e -> ((CampaignService.CampaignException) e).getErrorCode().equals("WORLD_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnknownSystem() {
        var world = new World("Aventurien", userId, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(worldId, gameSystemId, "X", userId))
            .isInstanceOf(CampaignService.CampaignException.class)
            .matches(e -> ((CampaignService.CampaignException) e).getErrorCode().equals("GAME_SYSTEM_NOT_FOUND"));
    }

    @Test
    void shouldRejectAccessDenied() {
        var world = new World("Aventurien", UUID.randomUUID(), "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doThrow(new WorldAccessException("WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, userId);

        assertThatThrownBy(() -> service.create(worldId, gameSystemId, "X", userId))
            .isInstanceOf(WorldAccessException.class);
    }

    @Test
    void shouldUpdateNameAndState() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(memberService.isDm(campaign.getId(), userId)).thenReturn(true);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(campaign.getId(), userId, "Runde 2", "{\"war\":\"active\"}", null);

        assertThat(updated.getName()).isEqualTo("Runde 2");
        assertThat(updated.getStateJson()).isEqualTo("{\"war\":\"active\"}");
    }

    @Test
    void shouldRejectUpdateNonMember() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        doThrow(new WorldAccessException("WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, userId);

        assertThatThrownBy(() -> service.update(campaign.getId(), userId, null, null, null))
            .isInstanceOf(WorldAccessException.class);
    }

    @Test
    void botModeValidation() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(memberService.isDm(campaign.getId(), userId)).thenReturn(true);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(campaign.getId(), userId, null, null, "autonom");
        assertThat(updated.getSettingsJson()).contains("autonom");

        assertThatThrownBy(() -> service.update(campaign.getId(), userId, null, null, "chaos"))
            .isInstanceOf(CampaignService.CampaignException.class)
            .matches(e -> ((CampaignService.CampaignException) e).getErrorCode().equals("INVALID_AI_MODE"));
    }

    @Test
    void pullSystemUpdatesPin() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        campaign.setRulesJsonSnapshot("{\"marker\":\"alt\"}");
        campaign.setGameSystemVersion(1);
        var system = new GameSystem("DSA", 4, "{\"marker\":\"v4\"}", "{}");
        setId(system, gameSystemId);
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(memberService.isDm(campaign.getId(), userId)).thenReturn(true);
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.pullSystem(campaign.getId(), userId);

        assertThat(updated.getGameSystemVersion()).isEqualTo(4);
        assertThat(updated.getRulesJsonSnapshot()).isEqualTo("{\"marker\":\"v4\"}");
    }

    @Test
    void shouldDeleteCampaign() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(memberService.isDm(campaign.getId(), userId)).thenReturn(true);

        service.delete(campaign.getId(), userId);

        verify(repo).delete(campaign);
        verify(worldAccess).requireAccess(worldId, userId);
    }

    @Test
    void shouldGetById() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));

        var result = service.getById(campaign.getId(), userId);

        assertThat(result.getId()).isEqualTo(campaign.getId());
        verify(worldAccess).requireAccess(worldId, userId);
    }

    @Test
    void listAccessibleOnlyReturnsCampaignsFromOwnedOrMemberWorlds() {
        var ownedWorld = new World("Mine", userId, "{}");
        setId(ownedWorld, worldId);
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        when(worldRepo.findByOwnerIdAndActiveTrue(userId)).thenReturn(List.of(ownedWorld));
        when(memberRepo.findWorldIdsByUserId(userId)).thenReturn(List.of());
        when(repo.findByWorldIdIn(List.of(worldId))).thenReturn(List.of(campaign));

        var result = service.listAccessible(userId);

        assertThat(result).containsExactly(campaign);
        verify(repo).findByWorldIdIn(List.of(worldId));
    }

    @Test
    void listAccessibleReturnsEmptyWhenNoWorlds() {
        when(worldRepo.findByOwnerIdAndActiveTrue(userId)).thenReturn(List.of());
        when(memberRepo.findWorldIdsByUserId(userId)).thenReturn(List.of());

        var result = service.listAccessible(userId);

        assertThat(result).isEmpty();
        verify(repo, never()).findByWorldIdIn(any());
    }

    @Test
    void listAccessibleIncludesMemberWorlds() {
        var otherWorldId = UUID.randomUUID();
        var memberCampaign = new Campaign(otherWorldId, gameSystemId, "Gast");
        when(worldRepo.findByOwnerIdAndActiveTrue(userId)).thenReturn(List.of());
        when(memberRepo.findWorldIdsByUserId(userId)).thenReturn(List.of(otherWorldId));
        var otherWorld = new World("Gastwelt", UUID.randomUUID(), "{}");
        setId(otherWorld, otherWorldId);
        otherWorld.setVisibility("INVITE_ONLY");
        when(worldRepo.findAllById(List.of(otherWorldId))).thenReturn(List.of(otherWorld));
        when(repo.findByWorldIdIn(List.of(otherWorldId))).thenReturn(List.of(memberCampaign));

        var result = service.listAccessible(userId);

        assertThat(result).containsExactly(memberCampaign);
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
