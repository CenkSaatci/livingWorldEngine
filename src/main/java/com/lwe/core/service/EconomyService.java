package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Berechnet Preise basierend auf Orts-Wohlstand und NPC-Preis-Modifier.
 *
 * <p>Formel: {@code preis = basispreis × (1 + (wealth - 5) × 0.1) × npc.price_modifier}
 */
@Service
public class EconomyService {

    private final LocationRepository locationRepo;
    private final GameEntityRepository entityRepo;
    private final ObjectMapper objectMapper;

    public EconomyService(LocationRepository locationRepo, GameEntityRepository entityRepo,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.locationRepo = locationRepo;
        this.entityRepo = entityRepo;
    }

    /**
     * Marktpreise für einen Ort berechnen.
     * Basis-Items + Preise modifiziert durch Wohlstand + NPC-Modifier.
     */
    public List<Map<String, Object>> getMarketPrices(UUID locationId, UUID userId) {
        var loc = locationRepo.findById(locationId)
            .orElseThrow(() -> new RuntimeException("LOCATION_NOT_FOUND"));
        var wealthFactor = 1.0 + (loc.getWealth() - 5) * 0.1;

        var filter = "{\"location_id\":\"" + locationId + "\"}";
        var npcs = entityRepo.findByMetadataJsonFilter(filter);

        var market = new ArrayList<Map<String, Object>>();
        for (var npc : npcs) {
            try {
                var meta = objectMapper.readTree(npc.getMetadataJson());
                var offered = meta.path("services_offered");
                var priceMod = meta.path("price_modifier").asDouble(1.0);

                if (offered.isArray()) {
                    for (var s : offered) {
                        var serviceName = s.asText();
                        var basePrice = BASE_PRICES.getOrDefault(serviceName, 10);
                        var finalPrice = (int) Math.round(basePrice * wealthFactor * priceMod);

                        market.add(Map.of(
                            "service", serviceName,
                            "npc", npc.getName(),
                            "base_price", basePrice,
                            "final_price", finalPrice,
                            "wealth_factor", wealthFactor,
                            "price_modifier", priceMod
                        ));
                    }
                }
            } catch (Exception ignored) {}
        }

        return market;
    }

    private static final Map<String, Integer> BASE_PRICES = Map.ofEntries(
        Map.entry("sell_weapons", 25),
        Map.entry("repair", 15),
        Map.entry("buy_ore", 8),
        Map.entry("sell_potions", 30),
        Map.entry("training", 100),
        Map.entry("healing", 20),
        Map.entry("inn_stay", 5),
        Map.entry("buy_food", 3),
        Map.entry("sell_scrolls", 50),
        Map.entry("identification", 25)
    );
}
