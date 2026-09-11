package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Zentrale Quelle für das Laden von Regelwerken.
 * 3-Ebenen-Modell: Kampagne → GameSystem → rulesJson.
 * Welt-basierte Methoden bleiben als Übergangs-Fallback bis P26.
 */
@Service
public class RulesLoader {

    private final WorldRepository worldRepo;
    private final GameSystemRepository systemRepo;
    private final CampaignRepository campaignRepo;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> RULES_MAP = new TypeReference<>() {};

    public RulesLoader(WorldRepository worldRepo, GameSystemRepository systemRepo,
                       CampaignRepository campaignRepo, ObjectMapper objectMapper) {
        this.worldRepo = worldRepo;
        this.systemRepo = systemRepo;
        this.campaignRepo = campaignRepo;
        this.objectMapper = objectMapper;
    }

    /** Liefert das GameSystem einer Kampagne oder null.
     *  P27-T05: bei gepinnter Kampagne eine Snapshot-Sicht (rulesJson/Version aus dem Pin),
     *  damit auch Kampf/Rast/Level-Up die eingefrorene System-Version nutzen. */
    public GameSystem loadSystemByCampaign(UUID campaignId) {
        if (campaignId == null) return null;
        var campaign = campaignRepo.findById(campaignId).orElse(null);
        if (campaign == null) return null;
        var system = systemRepo.findById(campaign.getGameSystemId()).orElse(null);
        if (system == null) return null;
        var snapshot = campaign.getRulesJsonSnapshot();
        if (snapshot != null && !snapshot.isBlank()) {
            var view = new GameSystem(system.getName(),
                campaign.getGameSystemVersion() != null ? campaign.getGameSystemVersion() : system.getVersion(),
                snapshot, system.getSchemaJson());
            view.setActive(system.isActive());
            return view;
        }
        return system;
    }

    /** true, wenn die Kampagne existiert und zur angegebenen Welt gehört (Audit P28). */
    public boolean campaignBelongsToWorld(UUID campaignId, UUID worldId) {
        if (campaignId == null || worldId == null) return false;
        return campaignRepo.findById(campaignId)
            .map(c -> worldId.equals(c.getWorldId()))
            .orElse(false);
    }

    /** Liefert die rulesJson einer Kampagne als Map oder leere Map.
     *  P27-T05: gepinnter Snapshot hat Vorrang vor der lebenden System-Zeile. */
    public Map<String, Object> loadRulesByCampaign(UUID campaignId) {
        if (campaignId == null) return Map.of();
        var campaign = campaignRepo.findById(campaignId).orElse(null);
        if (campaign == null) return Map.of();
        var snapshot = campaign.getRulesJsonSnapshot();
        if (snapshot != null && !snapshot.isBlank()) {
            try {
                return objectMapper.readValue(snapshot, RULES_MAP);
            } catch (Exception e) {
                return Map.of();
            }
        }
        var system = systemRepo.findById(campaign.getGameSystemId()).orElse(null);
        if (system == null || system.getRulesJson() == null || system.getRulesJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(system.getRulesJson(), RULES_MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Kombiniert-Pfad: Kampagne bevorzugt, sonst Welt-Fallback.
     * Liefert die rulesJson zur campaignId bzw. worldId als Map oder leere Map.
     */
    public Map<String, Object> loadRules(UUID campaignId, UUID worldId) {
        var byCampaign = loadRulesByCampaign(campaignId);
        if (!byCampaign.isEmpty()) return byCampaign;
        return loadRules(worldId);
    }

    /** Welten tragen seit P25-T06 kein System mehr — immer null (Kontext: Kampagne). */
    public GameSystem loadSystem(World world) {
        return null;
    }

    /** Liefert das GameSystem zur worldId oder null. */
    public GameSystem loadSystem(UUID worldId) {
        if (worldId == null) return null;
        return loadSystem(worldRepo.findById(worldId).orElse(null));
    }

    /** Liefert die rulesJson als Map oder leere Map bei Fehler/fehlendem System. */
    public Map<String, Object> loadRules(World world) {
        var system = loadSystem(world);
        if (system == null || system.getRulesJson() == null || system.getRulesJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(system.getRulesJson(), RULES_MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Liefert die rulesJson zur worldId als Map oder leere Map. */
    public Map<String, Object> loadRules(UUID worldId) {
        return loadRules(worldRepo.findById(worldId).orElse(null));
    }
}
