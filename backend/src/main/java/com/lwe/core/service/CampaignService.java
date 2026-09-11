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
    private final WorldService worldService;
    private final GameSystemService gameSystemService;

    public CampaignService(CampaignRepository repo,
                           WorldRepository worldRepo,
                           WorldMemberRepository memberRepo,
                           GameSystemRepository systemRepo,
                           WorldAccess worldAccess,
                           CampaignMemberService memberService,
                           WorldService worldService,
                           GameSystemService gameSystemService) {
        this.repo = repo;
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
        this.systemRepo = systemRepo;
        this.worldAccess = worldAccess;
        this.memberService = memberService;
        this.worldService = worldService;
        this.gameSystemService = gameSystemService;
    }

    /** Erstellt die Kampagne auf einer eigenen Welt-Kopie (Fork, P27-T03). */
    @Transactional
    public Campaign create(UUID worldId, UUID gameSystemId, String name, UUID userId) {
        worldRepo.findById(worldId)
            .orElseThrow(() -> new CampaignException("WORLD_NOT_FOUND", "World not found"));
        worldAccess.requireAccess(worldId, userId);
        var system = systemRepo.findById(gameSystemId)
            .filter(GameSystem::isActive)
            .orElseThrow(() -> new CampaignException("GAME_SYSTEM_NOT_FOUND", "Game system not found or inactive"));
        // F8/P27: fremde PRIVATE Systeme sind fuer Kampagnen nicht nutzbar.
        gameSystemService.requireUsableForCampaign(gameSystemId, userId, false);

        var fork = worldService.cloneForCampaign(worldId, userId);
        var campaign = new Campaign(fork.getId(), gameSystemId, name);
        campaign.setForkedWorld(true);
        campaign.setRulesJsonSnapshot(system.getRulesJson());
        campaign.setGameSystemVersion(system.getVersion());
        campaign = repo.save(campaign);
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
        requireCampaignDmOrWorldOwner(campaign, userId);
        if (name != null) campaign.setName(name);
        if (stateJson != null) campaign.setStateJson(stateJson);
        return repo.save(campaign);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var campaign = getById(id, userId);
        requireCampaignDmOrWorldOwner(campaign, userId);
        // Fork-Welt der Kampagne mit entfernen (soft-delete), Template bleibt unberuehrt.
        if (campaign.isForkedWorld()) {
            worldRepo.findById(campaign.getWorldId()).ifPresent(w -> {
                w.setActive(false);
                worldRepo.save(w);
            });
        }
        repo.delete(campaign);
    }

    /** P27-T05: System-Nachziehen — laufende Kampagne auf die aktuelle System-Version heben. */
    @Transactional
    public Campaign pullSystem(UUID id, UUID userId) {
        var campaign = getById(id, userId);
        requireCampaignDmOrWorldOwner(campaign, userId);
        var system = systemRepo.findById(campaign.getGameSystemId())
            .orElseThrow(() -> new CampaignException("GAME_SYSTEM_NOT_FOUND", "Game system not found"));
        campaign.setRulesJsonSnapshot(system.getRulesJson());
        campaign.setGameSystemVersion(system.getVersion());
        return repo.save(campaign);
    }

    /** Aenderungen nur DM der Kampagne oder Welt-Owner (Audit P27). */
    private void requireCampaignDmOrWorldOwner(Campaign campaign, UUID userId) {
        var isWorldOwner = worldRepo.findById(campaign.getWorldId())
            .map(w -> w.getOwnerId().equals(userId)).orElse(false);
        if (isWorldOwner) return;
        if (!memberService.isDm(campaign.getId(), userId)) {
            throw new CampaignException("DM_REQUIRED", "Only the DM may modify this campaign");
        }
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
