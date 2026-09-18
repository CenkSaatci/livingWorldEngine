package com.lwe.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Einheitliches Parsen der Entity-JSON-Felder ({@code attributes_json},
 * {@code skills_json}, {@code metadata_json.traits}) — ersetzt die früheren
 * Kopien in Probe-/Sheet-/Combat-/RestService.
 *
 * <p>ADR-014: korruptes JSON wird geloggt und fällt auf saubere Defaults zurück
 * (kein stiller Erfolg, aber auch kein 500er auf dem Bogen).
 */
public final class EntityJson {

    private static final Logger log = LoggerFactory.getLogger(EntityJson.class);
    private static final TypeReference<Map<String, Integer>> INT_MAP = new TypeReference<>() {};

    private EntityJson() {}

    public static Map<String, Integer> attributes(ObjectMapper mapper, String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank() || "{}".equals(attributesJson.trim())) {
            return Map.of();
        }
        try {
            return mapper.readValue(attributesJson, INT_MAP);
        } catch (Exception e) {
            log.warn("attributesJson nicht lesbar: {}", e.getMessage());
            return Map.of();
        }
    }

    public static Map<String, Integer> skills(ObjectMapper mapper, String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank() || "{}".equals(skillsJson.trim())) {
            return Map.of();
        }
        try {
            return mapper.readValue(skillsJson, INT_MAP);
        } catch (Exception e) {
            log.warn("skillsJson nicht lesbar (Fallback auf Regel-Bonus): {}", e.getMessage());
            return Map.of();
        }
    }

    public static List<String> traits(ObjectMapper mapper, String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return List.of();
        try {
            var node = mapper.readTree(metadataJson).path("traits");
            if (!node.isArray()) return List.of();
            var out = new ArrayList<String>();
            node.forEach(n -> {
                if (n.isTextual()) out.add(n.asText());
            });
            return out;
        } catch (Exception e) {
            log.warn("metadataJson.traits nicht lesbar: {}", e.getMessage());
            return List.of();
        }
    }
}
