package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.EconomyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations/{locationId}/market")
public class MarketController {

    private final EconomyService economyService;

    public MarketController(EconomyService economyService) {
        this.economyService = economyService;
    }

    @GetMapping
    public ResponseEntity<?> getMarket(@PathVariable UUID locationId,
                                       @AuthenticationPrincipal User user) {
        var prices = economyService.getMarketPrices(locationId, user.getId());
        return ResponseEntity.ok(prices);
    }
}
