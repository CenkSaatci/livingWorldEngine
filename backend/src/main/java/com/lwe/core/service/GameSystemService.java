package com.lwe.core.service;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.CampaignMemberRepository;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.rules.RuleSchemaValidator;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GameSystemService {

    private final GameSystemRepository repo;
    private final RuleSchemaValidator validator;
    private final CampaignRepository campaignRepo;
    private final CampaignMemberRepository campaignMemberRepo;
    private final WorldRepository worldRepo;

    public GameSystemService(GameSystemRepository repo, RuleSchemaValidator validator,
                             CampaignRepository campaignRepo,
                             CampaignMemberRepository campaignMemberRepo,
                             WorldRepository worldRepo) {
        this.repo = repo;
        this.validator = validator;
        this.campaignRepo = campaignRepo;
        this.campaignMemberRepo = campaignMemberRepo;
        this.worldRepo = worldRepo;
    }

    @PostConstruct
    public void seedDefaultSystems() {
        if (repo.count() > 0) return;

        var d20Rules = "{\"version\":1,\"attributes\":[{\"name\":\"staerke\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10},{\"name\":\"geschicklichkeit\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10},{\"name\":\"konstitution\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10},{\"name\":\"intelligenz\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10},{\"name\":\"weisheit\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10},{\"name\":\"charisma\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10}],\"skills\":[{\"name\":\"athletik\",\"attribute\":\"staerke\"},{\"name\":\"schleichen\",\"attribute\":\"geschicklichkeit\"},{\"name\":\"wahrnehmung\",\"attribute\":\"weisheit\"},{\"name\":\"ueberzeugen\",\"attribute\":\"charisma\"}],\"dice_mechanics\":{\"probe\":\"1d20+mod\",\"combat\":{\"initiative\":\"1d20+geschicklichkeit\",\"damage\":\"1d8+staerke\",\"action_points\":{\"standard\":1,\"max\":2}}}}";

        var poolRules = "{\"version\":1,\"attributes\":[{\"name\":\"staerke\",\"type\":\"INT\",\"min\":1,\"max\":12,\"default\":6},{\"name\":\"geschick\",\"type\":\"INT\",\"min\":1,\"max\":12,\"default\":6},{\"name\":\"verstand\",\"type\":\"INT\",\"min\":1,\"max\":12,\"default\":6}],\"skills\":[{\"name\":\"kaempfen\",\"attribute\":\"staerke\"},{\"name\":\"schleichen\",\"attribute\":\"geschick\"},{\"name\":\"wissen\",\"attribute\":\"verstand\"}],\"dice_mechanics\":{\"probe\":\"2d6+mod\",\"combat\":{\"initiative\":\"2d6+geschick\",\"damage\":\"1d6+staerke\",\"action_points\":{\"standard\":2,\"max\":4}}}}";

        var schema = "{}";

        try {
            create("D20Lite", 1, d20Rules, schema);
            create("TwoDicePool", 1, poolRules, schema);
        } catch (Exception e) {
            // Ignore if already exists
        }
    }

    /**
     * Neues Regelwerk anlegen. {@code rulesJson} wird vor Persistenz gegen
     * {@code schemaJson} validiert. Ohne Owner (Seed/Legacy) = PUBLIC, sonst PRIVATE.
     */
    @Transactional
    public GameSystem create(String name, int version, String rulesJson, String schemaJson) {
        return create(name, version, rulesJson, schemaJson, null);
    }

    @Transactional
    public GameSystem create(String name, int version, String rulesJson, String schemaJson, UUID ownerId) {
        if (repo.existsByName(name)) {
            throw new GameSystemException("GAME_SYSTEM_VERSION_CONFLICT",
                "A game system with name '" + name + "' already exists");
        }
        var schema = "{}".equals(schemaJson) || schemaJson == null
            ? RuleSchemaValidator.DEFAULT_SCHEMA : schemaJson;
        validator.validateOrThrow(rulesJson, schema);
        var gs = new GameSystem(name, version, rulesJson, schema, ownerId);
        return repo.save(gs);
    }

    /** F8/P27-T01: Fremdsystem-Schreibzugriff verhindern (Abilities/Items/Update). */
    public void requireOwnerForSystem(UUID systemId, UUID userId, boolean isAdmin) {
        var gs = getById(systemId);
        requireOwner(gs, userId, isAdmin);
    }

    /** Lesen: Owner, Admin, PUBLIC/Legacy oder Nutzer einer Kampagne mit diesem System. */
    public boolean canRead(GameSystem gs, UUID userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (gs.getOwnerId() != null && gs.getOwnerId().equals(userId)) return true;
        if ("PUBLIC".equals(gs.getVisibility())) return true;
        for (var c : campaignRepo.findByGameSystemId(gs.getId())) {
            if (campaignMemberRepo.existsByCampaignIdAndUserId(c.getId(), userId)) return true;
            var owner = worldRepo.findById(c.getWorldId())
                .map(w -> w.getOwnerId().equals(userId)).orElse(false);
            if (owner) return true;
        }
        return false;
    }

    public GameSystem getReadable(UUID id, UUID userId, boolean isAdmin) {
        var gs = getById(id);
        if (!canRead(gs, userId, isAdmin)) {
            throw new GameSystemException("GAME_SYSTEM_ACCESS_DENIED",
                "You may not read this game system");
        }
        return gs;
    }

    /** Fuer Kampagnen nutzbar: eigene, PUBLIC oder Legacy. */
    public void requireUsableForCampaign(UUID systemId, UUID userId, boolean isAdmin) {
        var gs = getById(systemId);
        if (isAdmin) return;
        boolean ok = "PUBLIC".equals(gs.getVisibility())
            || gs.getOwnerId() == null
            || gs.getOwnerId().equals(userId);
        if (!ok) {
            throw new GameSystemException("GAME_SYSTEM_ACCESS_DENIED",
                "This game system is private");
        }
    }

    /** F8/P27-T01: Schreiben nur Owner oder Admin; Legacy (Owner NULL) nur Admin. */
    private void requireOwner(GameSystem gs, UUID userId, boolean isAdmin) {
        if (isAdmin) return;
        if (gs.getOwnerId() == null || !gs.getOwnerId().equals(userId)) {
            throw new GameSystemException("GAME_SYSTEM_ACCESS_DENIED",
                "Only the owner may modify this game system");
        }
    }

    /**
     * Alle aktiven Regelwerke abrufen.
     */
    public List<GameSystem> listActive() {
        return repo.findByActiveTrue();
    }

    /** Fuer den User sichtbare Systeme (ADMIN sieht alle). */
    public List<GameSystem> listVisible(UUID userId, boolean isAdmin) {
        if (isAdmin) return repo.findByActiveTrue();
        return repo.findVisibleForUser(userId);
    }

    /**
     * Regelwerk per ID laden.
     */
    public GameSystem getById(UUID id) {
        return repo.findById(id)
            .orElseThrow(() -> new GameSystemException("GAME_SYSTEM_NOT_FOUND",
                "Game system not found: " + id));
    }

    /**
     * Regelwerk erneut validieren (ohne Persistenzänderung).
     */
    public List<RuleSchemaValidator.ValidationError> revalidate(UUID id) {
        var gs = getById(id);
        return validator.validate(gs.getRulesJson(), gs.getSchemaJson());
    }

    @Transactional
    public GameSystem update(UUID id, String name, Integer version, String rulesJson,
                             UUID userId, boolean isAdmin) {
        var gs = getById(id);
        requireOwner(gs, userId, isAdmin);
        if (name != null) gs.setName(name);
        if (version != null) gs.setVersion(version);
        if (rulesJson != null) {
            validator.validateOrThrow(rulesJson, gs.getSchemaJson());
            gs.setRulesJson(rulesJson);
        }
        return repo.save(gs);
    }

    @Transactional
    public void delete(UUID id, UUID userId, boolean isAdmin) {
        var gs = getById(id);
        requireOwner(gs, userId, isAdmin);
        gs.setActive(false);
        repo.save(gs);
    }

    @Transactional
    public GameSystem clone(UUID id, UUID userId, boolean isAdmin) {
        var original = getById(id);
        if (!isAdmin
            && !"PUBLIC".equals(original.getVisibility())
            && original.getOwnerId() != null
            && !original.getOwnerId().equals(userId)) {
            throw new GameSystemException("GAME_SYSTEM_ACCESS_DENIED",
                "You may not clone this game system");
        }
        var copy = new GameSystem(original.getName() + " (Copy)", original.getVersion(),
            original.getRulesJson(), original.getSchemaJson(), userId);
        // Ensure unique name
        if (repo.existsByName(copy.getName())) {
            copy.setName(copy.getName() + " " + System.currentTimeMillis());
        }
        return repo.save(copy);
    }

    public static class GameSystemException extends RuntimeException {
        private final String errorCode;
        public GameSystemException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}