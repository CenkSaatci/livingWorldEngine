package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private final CampaignRepository repo;
    private final WorldRepository worldRepo;
    private final WorldMemberRepository memberRepo;
    private final GameSystemRepository systemRepo;
    private final WorldAccess worldAccess;
    private final CampaignMemberService memberService;

    public CampaignService(CampaignRepository repo,
                           WorldRepository worldRepo,
                           WorldMemberRepository memberRepo,
                           GameSystemRepository systemRepo,
                           WorldAccess worldAccess,
                           CampaignMemberService memberService) {
        this.repo = repo;
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
        this.systemRepo = systemRepo;
        this.worldAccess = worldAccess;
        this.memberService = memberService;
    }

    @Transactional
    public Campaign create(UUID worldId, UUID gameSystemId, String name, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new CampaignException("WORLD_NOT_FOUND", "World not found"));
        worldAccess.requireAccess(worldId, userId);
        systemRepo.findById(gameSystemId)
            .filter(GameSystem::isActive)
            .orElseThrow(() -> new CampaignException("GAME_SYSTEM_NOT_FOUND", "Game system not found or inactive"));

        var campaign = repo.save(new Campaign(worldId, gameSystemId, name));
        memberService.addCreatorAsDm(campaign, userId);
        return campaign;
    }

    public List<Campaign> listByWorld(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
        return repo.findByWorldId(worldId);
    }

    /** Kampagnen aus Welten, auf die der User Zugriff hat (Owner + Member). */
    public List<Campaign> listAccessible(UUID userId) {
        var owned = worldRepo.findByOwnerIdAndActiveTrue(userId).stream()
            .map(World::getId).toList();
        var memberWorldIds = memberRepo.findWorldIdsByUserId(userId);
        var worldIds = java.util.stream.Stream.concat(owned.stream(), memberWorldIds.stream())
            .distinct().toList();
        if (worldIds.isEmpty()) return List.of();
        return repo.findByWorldIdIn(worldIds);
    }

    public Campaign getById(UUID id, UUID userId) {
        var campaign = repo.findById(id)
            .orElseThrow(() -> new CampaignException("CAMPAIGN_NOT_FOUND", "Campaign not found"));
        worldAccess.requireAccess(campaign.getWorldId(), userId);
        return campaign;
    }

    @Transactional
    public Campaign update(UUID id, UUID userId, String name, String stateJson) {
        var campaign = getById(id, userId);
        if (name != null) campaign.setName(name);
        if (stateJson != null) campaign.setStateJson(stateJson);
        return repo.save(campaign);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var campaign = getById(id, userId);
        repo.delete(campaign);
    }

    public static class CampaignException extends RuntimeException {
        private final String errorCode;
        public CampaignException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
