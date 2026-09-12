package com.lwe.core.service;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.CampaignMemberRepository;
import com.lwe.core.repository.GameSystemShareRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.domain.GameSystemShare;
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
    private final GameSystemShareRepository shareRepo;
    private final UserRepository userRepo;

    public GameSystemService(GameSystemRepository repo, RuleSchemaValidator validator,
                             CampaignRepository campaignRepo,
                             CampaignMemberRepository campaignMemberRepo,
                             WorldRepository worldRepo,
                             GameSystemShareRepository shareRepo,
                             UserRepository userRepo) {
        this.repo = repo;
        this.validator = validator;
        this.campaignRepo = campaignRepo;
        this.campaignMemberRepo = campaignMemberRepo;
        this.worldRepo = worldRepo;
        this.shareRepo = shareRepo;
        this.userRepo = userRepo;
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
        // Audit T7: Custom-Schemas sind nicht Produkt-Scope (update/revalidate nutzen
        // ohnehin das Default-Schema) — immer ggue. dem aktuellen Default validieren.
        var schema = RuleSchemaValidator.DEFAULT_SCHEMA;
        validator.validateOrThrow(rulesJson, schema);
        var gs = new GameSystem(name, version, rulesJson, schema, ownerId);
        // Seeds/Legacy (kein Owner) bleiben global sichtbar (Audit P27).
        if (ownerId == null) gs.setVisibility("PUBLIC");
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
        if (shareRepo.existsBySystemIdAndUserId(gs.getId(), userId)) return true; // T33-05
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

    // ---- T33-05: Shares (INVITE_ONLY) ----

    public List<GameSystemShare> listShares(UUID systemId, UUID userId, boolean isAdmin) {
        var gs = getById(systemId);
        requireOwner(gs, userId, isAdmin);
        return shareRepo.findBySystemId(systemId);
    }

    @Transactional
    public GameSystemShare share(UUID systemId, UUID actorId, boolean isAdmin, String emailOrUsername) {
        var gs = getById(systemId);
        requireOwner(gs, actorId, isAdmin);
        var target = userRepo.findByEmail(emailOrUsername)
            .or(() -> userRepo.findByUsername(emailOrUsername))
            .orElseThrow(() -> new GameSystemException("USER_NOT_FOUND", "User not found"));
        if (shareRepo.existsBySystemIdAndUserId(systemId, target.getId())) {
            throw new GameSystemException("GAME_SYSTEM_SHARE_EXISTS", "Already shared with this user");
        }
        if (target.getId().equals(gs.getOwnerId())) {
            throw new GameSystemException("GAME_SYSTEM_SHARE_SELF", "Cannot share with yourself");
        }
        // Audit Block B: PUBLIC nicht still downgraden — erst Sichtbarkeit aendern.
        if ("PUBLIC".equals(gs.getVisibility())) {
            throw new GameSystemException("GAME_SYSTEM_PUBLIC_NO_SHARE_NEEDED",
                "Public systems are readable by everyone; set INVITE_ONLY first");
        }
        return shareRepo.save(new GameSystemShare(systemId, target.getId()));
    }

    @Transactional
    public void unshare(UUID systemId, UUID actorId, boolean isAdmin, UUID targetUserId) {
        var gs = getById(systemId);
        requireOwner(gs, actorId, isAdmin);
        var share = shareRepo.findBySystemIdAndUserId(systemId, targetUserId)
            .orElseThrow(() -> new GameSystemException("GAME_SYSTEM_SHARE_NOT_FOUND", "Share not found"));
        shareRepo.delete(share);
    }

    /** Lesen fremder System-Inhalte (Abilities/Items) unterbinden. */
    public void requireReadableForSystem(UUID systemId, UUID userId, boolean isAdmin) {
        var gs = getById(systemId);
        if (!canRead(gs, userId, isAdmin)) {
            throw new GameSystemException("GAME_SYSTEM_ACCESS_DENIED",
                "You may not read this game system");
        }
    }

    /** Fuer Kampagnen nutzbar: eigene, PUBLIC oder Legacy. */
    public void requireUsableForCampaign(UUID systemId, UUID userId, boolean isAdmin) {
        var gs = getById(systemId);
        if (isAdmin) return;
        boolean ok = "PUBLIC".equals(gs.getVisibility())
            || gs.getOwnerId() == null
            || gs.getOwnerId().equals(userId)
            || shareRepo.existsBySystemIdAndUserId(gs.getId(), userId); // T33-05
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
        return validator.validate(gs.getRulesJson(), RuleSchemaValidator.DEFAULT_SCHEMA);
    }

    @Transactional
    public GameSystem update(UUID id, String name, Integer version, String rulesJson,
                             UUID userId, boolean isAdmin) {
        var gs = getById(id);
        requireOwner(gs, userId, isAdmin);
        if (name != null) gs.setName(name);
        if (version != null) gs.setVersion(version);
        if (rulesJson != null && !rulesJson.equals(gs.getRulesJson())) {
            // Live-Fund T7: gespeicherte schemaJson ist eine Kopie des Defaults zur
            // Erstellungszeit und damit veraltet — immer gegen das aktuelle Default-Schema
            // validieren (Custom-Schemas sind im Produkt nicht vorgesehen).
            validator.validateOrThrow(rulesJson, RuleSchemaValidator.DEFAULT_SCHEMA);
            gs.setRulesJson(rulesJson);
            // Audit P27: Regel-Aenderung erhoeht die Version automatisch (monoton).
            gs.setVersion(gs.getVersion() + 1);
        } else if (version != null && version > gs.getVersion()) {
            gs.setVersion(version);
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