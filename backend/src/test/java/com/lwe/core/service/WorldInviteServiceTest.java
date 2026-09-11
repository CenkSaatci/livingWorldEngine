package com.lwe.core.service;

import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.domain.WorldInvite;
import com.lwe.core.repository.WorldInviteRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldInviteServiceTest {

    @Mock private WorldInviteRepository inviteRepo;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldMemberRepository memberRepo;

    private WorldInviteService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorldInviteService(inviteRepo, worldRepo, memberRepo);
    }

    @Test
    void shouldCreateInvite() {
        var world = new World("Test", userId, "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(inviteRepo.save(any())).thenAnswer(inv -> {
            var i = inv.<WorldInvite>getArgument(0);
            setId(i, UUID.randomUUID());
            return i;
        });

        var invite = service.create(worldId, userId, 1, null);

        assertThat(invite.getToken()).isNotNull();
        assertThat(invite.getMaxUses()).isEqualTo(1);
        assertThat(invite.getWorldId()).isEqualTo(worldId);
        verify(inviteRepo).save(any());
    }

    @Test
    void shouldRejectCreateByNonOwner() {
        var world = new World("Test", UUID.randomUUID(), "{}");
        setId(world, worldId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));

        assertThatThrownBy(() -> service.create(worldId, userId, 1, null))
            .isInstanceOf(WorldInviteService.InviteException.class)
            .matches(e -> ((WorldInviteService.InviteException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    @Test
    void shouldJoinViaToken() {
        var token = "valid-token-123";
        var invite = new WorldInvite(worldId, userId, token, 1);
        setId(invite, UUID.randomUUID());
        invite.setUseCount(0);

        when(inviteRepo.findByToken(token)).thenReturn(Optional.of(invite));
        when(memberRepo.existsByWorldIdAndUserId(worldId, userId)).thenReturn(false);

        service.join(token, userId);

        assertThat(invite.getUseCount()).isEqualTo(1);
        verify(memberRepo).save(any());
    }

    @Test
    void shouldRejectExpiredToken() {
        var token = "expired-token";
        var invite = new WorldInvite(worldId, userId, token, 1);
        setId(invite, UUID.randomUUID());
        invite.setExpiresAt(Instant.now().minusSeconds(3600));

        when(inviteRepo.findByToken(token)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.join(token, userId))
            .isInstanceOf(WorldInviteService.InviteException.class)
            .matches(e -> ((WorldInviteService.InviteException) e).getErrorCode().equals("INVITE_EXPIRED"));
    }

    @Test
    void shouldRejectUsedUpToken() {
        var token = "used-token";
        var invite = new WorldInvite(worldId, userId, token, 1);
        setId(invite, UUID.randomUUID());
        invite.setUseCount(1);

        when(inviteRepo.findByToken(token)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.join(token, userId))
            .isInstanceOf(WorldInviteService.InviteException.class)
            .matches(e -> ((WorldInviteService.InviteException) e).getErrorCode().equals("INVITE_EXHAUSTED"));
    }

    @Test
    void shouldRejectAlreadyMember() {
        var token = "member-token";
        var invite = new WorldInvite(worldId, userId, token, 1);
        setId(invite, UUID.randomUUID());
        invite.setUseCount(0);

        when(inviteRepo.findByToken(token)).thenReturn(Optional.of(invite));
        when(memberRepo.existsByWorldIdAndUserId(worldId, userId)).thenReturn(true);

        assertThatThrownBy(() -> service.join(token, userId))
            .isInstanceOf(WorldInviteService.InviteException.class)
            .matches(e -> ((WorldInviteService.InviteException) e).getErrorCode().equals("ALREADY_MEMBER"));
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
