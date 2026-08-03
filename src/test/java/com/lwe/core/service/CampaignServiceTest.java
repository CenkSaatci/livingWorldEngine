package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.util.WorldAccess.WorldAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock private GameSystemRepository systemRepo;
    @Mock private WorldAccess worldAccess;

    private CampaignService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignService(repo, worldRepo, systemRepo, worldAccess);
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
    }

    @Test
    void shouldCreateCampaign() {
        var world = new World("Aventurien", userId, null, "{}");
        var system = new GameSystem("DSA", 1, "{}", "{}");
        setId(world, worldId);
        setId(system, gameSystemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));
        when(repo.save(any())).thenAnswer(inv -> {
            var c = inv.<Campaign>getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        var campaign = service.create(worldId, gameSystemId, "Runde 1", userId);

        assertThat(campaign.getName()).isEqualTo("Runde 1");
        assertThat(campaign.getWorldId()).isEqualTo(worldId);
        assertThat(campaign.getGameSystemId()).isEqualTo(gameSystemId);
        verify(worldAccess).requireAccess(worldId, userId);
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
        var world = new World("Aventurien", userId, null, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(worldId, gameSystemId, "X", userId))
            .isInstanceOf(CampaignService.CampaignException.class)
            .matches(e -> ((CampaignService.CampaignException) e).getErrorCode().equals("GAME_SYSTEM_NOT_FOUND"));
    }

    @Test
    void shouldRejectAccessDenied() {
        var world = new World("Aventurien", UUID.randomUUID(), null, "{}");
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
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(campaign.getId(), userId, "Runde 2", "{\"war\":\"active\"}");

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

        assertThatThrownBy(() -> service.update(campaign.getId(), userId, null, null))
            .isInstanceOf(WorldAccessException.class);
    }

    @Test
    void shouldDeleteCampaign() {
        var campaign = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(campaign, UUID.randomUUID());
        when(repo.findById(campaign.getId())).thenReturn(Optional.of(campaign));

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

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
