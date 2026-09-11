package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lwe.core.domain.GameEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Zustände/Status (P29-T01): Katalog im Regelwerk ({@code conditions[]}),
 * Instanzen am Charakter in {@code metadataJson.conditions} (String oder
 * {name, rounds}). Effekte nutzen dieselbe Semantik wie Traits
 * ({@code target}/{@code op}/{@code value}), Targets hier: {@code probe},
 * {@code damage} (frei erweiterbar).
 */
@Service
public class ConditionService {

    private final ObjectMapper mapper;

    public ConditionService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public record ConditionInstance(String name, Integer rounds) {}

    /** Aktive Zustände des Charakters (String- und Objektform). */
    @SuppressWarnings("unchecked")
    public List<ConditionInstance> active(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return List.of();
        try {
            var node = mapper.readTree(entity.getMetadataJson()).path("conditions");
            if (!node.isArray()) return List.of();
            var out = new ArrayList<ConditionInstance>();
            for (var n : node) {
                if (n.isTextual()) {
                    out.add(new ConditionInstance(n.asText(), null));
                } else if (n.isObject() && n.path("name").isTextual()) {
                    var rounds = n.hasNonNull("rounds") ? n.get("rounds").asInt() : null;
                    out.add(new ConditionInstance(n.get("name").asText(), rounds));
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Summe der Effekte aktiver Zustände für ein Target (z. B. "probe", "damage"). */
    public int modifier(GameEntity entity, Map<String, Object> rules, String target) {
        var active = active(entity);
        if (active.isEmpty()) return 0;
        var catalog = conditions(rules);
        int sum = 0;
        for (var inst : active) {
            var def = catalog.stream()
                .filter(c -> inst.name().equals(c.get("name")))
                .findFirst().orElse(null);
            if (def == null) continue;
            if (!(def.get("effects") instanceof List<?> effects)) continue;
            for (var e : effects) {
                if (e instanceof Map<?, ?> m
                    && target.equals(m.get("target"))
                    && "add".equals(m.get("op"))
                    && m.get("value") instanceof Number n) {
                    sum += n.intValue();
                }
            }
        }
        return sum;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> conditions(Map<String, Object> rules) {
        var raw = rules.get("conditions");
        if (!(raw instanceof List<?> list)) return List.of();
        // Custom-Schemas koennen untypisierte Eintraege liefern (Audit): nur Maps.
        return list.stream()
            .filter(Map.class::isInstance)
            .map(m -> (Map<String, Object>) m)
            .toList();
    }

    /** Decrementiert Runden; abgelaufene Zustände fallen raus. Mutiert das Entity-Metadata. */
    public List<ConditionInstance> tick(GameEntity entity) {
        var active = active(entity);
        if (active.isEmpty() || active.stream().allMatch(c -> c.rounds() == null)) return active;
        var next = new ArrayList<ConditionInstance>();
        for (var c : active) {
            if (c.rounds() == null) {
                next.add(c);
                continue;
            }
            int remaining = c.rounds() - 1;
            if (remaining > 0) next.add(new ConditionInstance(c.name(), remaining));
        }
        write(entity, next);
        return next;
    }

    public void add(GameEntity entity, ConditionInstance instance) {
        var list = new ArrayList<>(active(entity));
        list.removeIf(c -> c.name().equals(instance.name()));
        list.add(instance);
        write(entity, list);
    }

    public void remove(GameEntity entity, String name) {
        var list = new ArrayList<>(active(entity));
        list.removeIf(c -> c.name().equals(name));
        write(entity, list);
    }

    private void write(GameEntity entity, List<ConditionInstance> list) {
        try {
            ObjectNode meta;
            if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) {
                meta = mapper.createObjectNode();
            } else {
                var parsed = mapper.readTree(entity.getMetadataJson());
                meta = parsed.isObject() ? (ObjectNode) parsed : mapper.createObjectNode();
            }
            ArrayNode arr = mapper.createArrayNode();
            for (var c : list) {
                if (c.rounds() == null) {
                    arr.add(c.name());
                } else {
                    var o = mapper.createObjectNode();
                    o.put("name", c.name());
                    o.put("rounds", c.rounds());
                    arr.add(o);
                }
            }
            meta.set("conditions", arr);
            entity.setMetadataJson(mapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update conditions", e);
        }
    }
}
