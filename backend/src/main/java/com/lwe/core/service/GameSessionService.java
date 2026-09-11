package com.lwe.core.service;

import com.lwe.core.domain.GameSession;
import com.lwe.core.repository.GameSessionRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.lwe.core.service.WorldEventService.EventType.*;

@Service
public class GameSessionService {

    private final GameSessionRepository sessionRepo;
    private final WorldRepository worldRepo;
    private final WorldEventService eventService;
    private final CampaignMemberService campaignMemberService;

    public GameSessionService(GameSessionRepository sessionRepo, WorldRepository worldRepo,
                              WorldEventService eventService,
                              CampaignMemberService campaignMemberService) {
        this.sessionRepo = sessionRepo;
        this.worldRepo = worldRepo;
        this.eventService = eventService;
        this.campaignMemberService = campaignMemberService;
    }

    @Transactional
    public GameSession startSession(UUID worldId, UUID campaignId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new SessionException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)
            && (campaignId == null || !campaignMemberService.isDm(campaignId, userId))) {
            throw new SessionException("WORLD_ACCESS_DENIED", "Access denied");
        }
        var session = new GameSession(worldId, campaignId);
        session = sessionRepo.save(session);
        eventService.publish(worldId, campaignId, SESSION_STARTED, null, null, Map.of(
            "sessionId", session.getId(),
            "campaignId", campaignId != null ? campaignId.toString() : "",
            "dm", userId.toString()
        ));
        return session;
    }

    @Transactional
    public GameSession endSession(UUID sessionId, UUID userId) {
        var session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new SessionException("SESSION_NOT_FOUND", "Session not found"));
        var world = worldRepo.findById(session.getWorldId())
            .orElseThrow(() -> new SessionException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)
            && (session.getCampaignId() == null || !campaignMemberService.isDm(session.getCampaignId(), userId))) {
            throw new SessionException("WORLD_ACCESS_DENIED", "Access denied");
        }
        session.setStatus("ENDED");
        session.setEndedAt(Instant.now());
        session = sessionRepo.save(session);
        eventService.publish(session.getWorldId(), session.getCampaignId(), SESSION_ENDED, null, null, Map.of(
            "sessionId", session.getId()
        ));
        return session;
    }

    public List<GameSession> listSessions(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new SessionException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new SessionException("WORLD_ACCESS_DENIED", "Access denied");
        return sessionRepo.findByWorldIdAndStatusOrderByStartedAtDesc(worldId, "ACTIVE");
    }

    public static class SessionException extends RuntimeException {
        private final String errorCode;
        public SessionException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
