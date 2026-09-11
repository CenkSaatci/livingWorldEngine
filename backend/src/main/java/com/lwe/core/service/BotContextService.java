package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * T33-06: Kontext fuer den AI-Bot — Welten + Kampagnen + effektive Modi.
 * Der Bot pollt danach statt ueber GET /worlds (listOwned).
 */
@Service
public class BotContextService {

    private final WorldRepository worldRepo;
    private final CampaignRepository campaignRepo;
    private final ObjectMapper objectMapper;

    public BotContextService(WorldRepository worldRepo, CampaignRepository campaignRepo,
                             ObjectMapper objectMapper) {
        this.worldRepo = worldRepo;
        this.campaignRepo = campaignRepo;
        this.objectMapper = objectMapper;
    }

    public record CampaignBotContext(UUID id, String name, String botMode) {}
    public record WorldBotContext(UUID worldId, String name, String worldAiMode,
                                  List<CampaignBotContext> campaigns) {}

    public List<WorldBotContext> listBotContexts() {
        return worldRepo.findByActiveTrue().stream().map(w -> {
            var campaigns = campaignRepo.findByWorldId(w.getId()).stream()
                .filter(c -> !c.isForkedWorld() || true) // alle Kampagnen der (Fork-)Welt
                .map(c -> new CampaignBotContext(c.getId(), c.getName(), readBotMode(c.getSettingsJson())))
                .toList();
            return new WorldBotContext(w.getId(), w.getName(), readWorldAiMode(w.getSettingsJson()),
                campaigns);
        }).toList();
    }

    private String readBotMode(String settingsJson) {
        return read(settingsJson, "bot", "mode");
    }

    private String readWorldAiMode(String settingsJson) {
        return read(settingsJson, "ai_mode");
    }

    private String read(String json, String... path) {
        if (json == null || json.isBlank()) return null;
        try {
            var node = objectMapper.readTree(json);
            for (var key : path) node = node.path(key);
            return node.isTextual() ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
