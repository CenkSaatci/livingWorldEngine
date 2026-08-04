package com.lwe.core.service;

import com.lwe.core.service.WorldEventService;
import com.lwe.core.domain.GameSession;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameSessionRepository;
import com.lwe.core.repository.WorldRepository;
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
class GameSessionServiceTest {

    @Mock private GameSessionRepository sessionRepo;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldEventService eventService;
    @Mock private CampaignMemberService campaignMemberService;

    private GameSessionService service;

    @BeforeEach
    void setUp() {
        service = new GameSessionService(sessionRepo, worldRepo, eventService, campaignMemberService);
    }

    @Test
    void startSessionShouldCreateSession() throws Exception {
        var worldId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var world = new World("test", userId, null, "{}");

        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<GameSession>getArgument(0);
            var idField = GameSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, UUID.randomUUID());
            return s;
        });

        var session = service.startSession(worldId, null, userId);

        assertThat(session.getWorldId()).isEqualTo(worldId);
        assertThat(session.getStatus()).isEqualTo("ACTIVE");
        verify(sessionRepo).save(any());
    }

    @Test
    void dmCanStartCampaignSession() throws Exception {
        var worldId = UUID.randomUUID();
        var campaignId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var world = new World("test", UUID.randomUUID(), null, "{}");

        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(campaignMemberService.isDm(campaignId, userId)).thenReturn(true);
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            var s = inv.<GameSession>getArgument(0);
            var idField = GameSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, UUID.randomUUID());
            return s;
        });

        var session = service.startSession(worldId, campaignId, userId);

        assertThat(session.getCampaignId()).isEqualTo(campaignId);
        verify(campaignMemberService).isDm(campaignId, userId);
    }

    @Test
    void nonDmNonOwnerCannotStartCampaignSession() {
        var worldId = UUID.randomUUID();
        var campaignId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var world = new World("test", UUID.randomUUID(), null, "{}");

        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(campaignMemberService.isDm(campaignId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.startSession(worldId, campaignId, userId))
            .isInstanceOf(GameSessionService.SessionException.class)
            .matches(e -> ((GameSessionService.SessionException) e).getErrorCode().equals("WORLD_ACCESS_DENIED"));
    }

    @Test
    void startSessionShouldThrowWhenWorldNotFound() {
        when(worldRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(UUID.randomUUID(), null, UUID.randomUUID()))
            .isInstanceOf(GameSessionService.SessionException.class)
            .matches(e -> ((GameSessionService.SessionException) e).getErrorCode().equals("WORLD_NOT_FOUND"));
    }

    @Test
    void endSessionShouldSetStatusToEnded() throws Exception {
        var worldId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var world = new World("test", userId, null, "{}");
        var session = new GameSession(worldId, null);
        var idField = GameSession.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(session, sessionId);

        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.<GameSession>getArgument(0));

        var result = service.endSession(sessionId, userId);

        assertThat(result.getStatus()).isEqualTo("ENDED");
        assertThat(result.getEndedAt()).isNotNull();
    }

    @Test
    void listSessionsShouldReturnActiveSessions() {
        var worldId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var world = new World("test", userId, null, "{}");
        var sessions = List.of(new GameSession(worldId, null), new GameSession(worldId, null));

        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(sessionRepo.findByWorldIdAndStatusOrderByStartedAtDesc(worldId, "ACTIVE"))
            .thenReturn(sessions);

        var result = service.listSessions(worldId, userId);

        assertThat(result).hasSize(2);
    }
}
