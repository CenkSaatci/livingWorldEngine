package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.GameEntityRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ADR-015: Bindet Katalog-Aktionen an einen Ort — über {@code location.services[]}
 * und {@code services_offered[]} der NPCs am Ort. Gemeinsame Quelle für
 * {@link PoiActionService} (Ausführen) und {@link MerchantService} (Handel).
 */
@Component
public class PoiBindings {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PoiBindings.class);

    private final GameEntityRepository entityRepo;
    private final ObjectMapper mapper;

    public PoiBindings(GameEntityRepository entityRepo, ObjectMapper mapper) {
        this.entityRepo = entityRepo;
        this.mapper = mapper;
    }

    /** Aktionsnamen, die an diesem Ort hängen (lowercase). */
    public Set<String> boundNames(Location location, UUID worldId) {
        var out = new LinkedHashSet<String>();
        try {
            if (location.getServices() != null && !location.getServices().isBlank()) {
                for (var node : mapper.readTree(location.getServices())) {
                    if (node.isTextual()) out.add(node.asText().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            log.warn("location.services nicht lesbar: {}", e.getMessage());
        }
        var filter = "{\"location_id\":\"" + location.getId() + "\"}";
        for (var npc : entityRepo.findByWorldIdAndMetadataJsonFilter(worldId, filter)) {
            try {
                var offered = mapper.readTree(npc.getMetadataJson()).path("services_offered");
                if (offered.isArray()) {
                    for (var s : offered) if (s.isTextual()) out.add(s.asText().toLowerCase(Locale.ROOT));
                }
            } catch (Exception ignored) {
                // NPC-Metadata kaputt → überspringen (Ort bleibt nutzbar)
            }
        }
        return out;
    }

    public static boolean isBound(String name, Set<String> bound) {
        return name != null && bound.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * {@code trade}-Block der ersten gebundenen Aktion mit Handel (oder null).
     * Ein leeres {@code trade: {}} erlaubt Kauf und Verkauf; nur explizites
     * {@code false} schaltet eine Richtung ab.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> tradeConfig(Map<String, Object> rules, Set<String> bound) {
        if (rules == null || !(rules.get("poi_actions") instanceof List<?> list)) return null;
        for (var raw : list) {
            if (!(raw instanceof Map<?, ?> m)) continue;
            if (!(m.get("name") instanceof String name) || !isBound(name, bound)) continue;
            if (m.get("trade") instanceof Map<?, ?> trade) return (Map<String, Object>) trade;
        }
        return null;
    }

    public static boolean tradeAllows(Map<String, Object> trade, boolean buying) {
        if (trade == null) return false;
        return !Boolean.FALSE.equals(trade.get(buying ? "buy" : "sell"));
    }
}
