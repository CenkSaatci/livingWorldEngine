package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lwe.core.domain.GameEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Währung (ADR-015): Der Charakter hält genau <b>einen</b> Basiswert
 * ({@code metadataJson.money}, kleinste Sorte, nie negativ). Die Sorten aus dem
 * Regel-{@code currency}-Block sind reine Darstellung — dadurch gibt es keinen
 * Wechselgeld-Algorithmus.
 *
 * <p>Fehlende Währungs-Config = nackte Zahl (rückwärtskompatibel). Kaputte
 * Sorten werden beim Parsen verworfen (Anzeige fällt auf die Zahl zurück),
 * die Engine rechnet immer in Basiseinheiten.
 */
@Service
public class CurrencyService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CurrencyService.class);

    private final ObjectMapper mapper;

    public CurrencyService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public record Denomination(String name, String abbr, int factor) {}

    /** Aktueller Basiswert des Charakters (Default 0). */
    public int money(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return 0;
        try {
            var node = mapper.readTree(entity.getMetadataJson()).path("money");
            return node.isNumber() ? node.asInt() : 0;
        } catch (Exception e) {
            log.warn("metadataJson.money nicht lesbar: {}", e.getMessage());
            return 0;
        }
    }

    /** Basiswert schreiben (nie negativ). Der Aufrufer speichert die Entity. */
    public void setMoney(GameEntity entity, int value) {
        if (value < 0) throw new CurrencyException("MONEY_INSUFFICIENT", "Money cannot go negative");
        try {
            ObjectNode meta;
            if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) {
                meta = mapper.createObjectNode();
            } else {
                var parsed = mapper.readTree(entity.getMetadataJson());
                meta = parsed.isObject() ? (ObjectNode) parsed : mapper.createObjectNode();
            }
            meta.put("money", value);
            entity.setMetadataJson(mapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update money", e);
        }
    }

    /** Reine Prüfung, keine Wirkung (H-2: erst validieren, dann anwenden). */
    public void requirePayable(GameEntity entity, int amount) {
        if (amount <= 0) return;
        if (money(entity) < amount) {
            throw new CurrencyException("MONEY_INSUFFICIENT",
                "Not enough money: needs " + amount + ", has " + money(entity));
        }
    }

    /** Nach erfolgreicher Vorprüfung abbuchen. */
    public void pay(GameEntity entity, int amount) {
        if (amount <= 0) return;
        requirePayable(entity, amount);
        setMoney(entity, money(entity) - amount);
    }

    /** Gutschreiben. */
    public void credit(GameEntity entity, int amount) {
        if (amount <= 0) return;
        setMoney(entity, money(entity) + amount);
    }

    /**
     * Sorten des Regelwerks, absteigend nach {@code factor}. Leer = keine Config.
     */
    @SuppressWarnings("unchecked")
    public List<Denomination> denominations(Map<String, Object> rules) {
        if (rules == null) return List.of();
        if (!(rules.get("currency") instanceof Map<?, ?> currency)) return List.of();
        if (!(currency.get("denominations") instanceof List<?> raw)) return List.of();
        var out = new ArrayList<Denomination>();
        for (var entry : raw) {
            if (!(entry instanceof Map<?, ?> d)) continue;
            var name = d.get("name") instanceof String n && !n.isBlank() ? n : null;
            var factor = d.get("factor") instanceof Number f ? f.intValue() : 0;
            if (name == null || factor < 1) continue;
            var abbr = d.get("abbr") instanceof String a && !a.isBlank() ? a : null;
            out.add(new Denomination(name, abbr, factor));
        }
        out.sort(Comparator.comparingInt(Denomination::factor).reversed());
        return out;
    }

    /** Anzeige, z. B. 312 → "3 G, 1 S, 2 K". Ohne Config die nackte Zahl. */
    public String format(int base, Map<String, Object> rules) {
        var parts = breakdown(base, rules);
        if (parts.isEmpty()) return String.valueOf(base);
        var sb = new StringBuilder();
        for (var part : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.get("count")).append(' ')
              .append(part.get("abbr") != null ? part.get("abbr") : part.get("name"));
        }
        return sb.toString();
    }

    /**
     * Sorten-Aufteilung fürs Frontend: {@code [{name, abbr, count}]}, nur Reste > 0.
     * Greedy absteigend; ohne Config leer (Aufrufer zeigt die Zahl).
     */
    public List<Map<String, Object>> breakdown(int base, Map<String, Object> rules) {
        var denominations = denominations(rules);
        if (denominations.isEmpty() || base == 0) return List.of();
        var out = new ArrayList<Map<String, Object>>();
        int remaining = base;
        for (var d : denominations) {
            int count = remaining / d.factor();
            if (count > 0) {
                var row = new java.util.LinkedHashMap<String, Object>();
                row.put("name", d.name());
                row.put("abbr", d.abbr());
                row.put("count", count);
                out.add(row);
                remaining -= count * d.factor();
            }
        }
        // L-4: Sorten ohne Faktor 1 (nicht-kanonisch) können einen Rest lassen.
        // Dann lieber die rohe Basiszahl zeigen als einen falschen Sortenbetrag.
        if (remaining > 0) return List.of();
        return out;
    }

    public static class CurrencyException extends RuntimeException {
        private final String errorCode;
        public CurrencyException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
