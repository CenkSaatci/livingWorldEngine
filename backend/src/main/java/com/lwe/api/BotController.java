package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.BotContextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * T33-06: interner Bot-Kontext. Nur Rolle BOT (oder ADMIN zu Debug-Zwecken).
 */
@RestController
@RequestMapping("/api/v1/bot")
public class BotController {

    private final BotContextService service;

    public BotController(BotContextService service) {
        this.service = service;
    }

    @GetMapping("/worlds")
    public ResponseEntity<List<BotContextService.WorldBotContext>> worlds(
            @AuthenticationPrincipal User user) {
        if (!"BOT".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BOT role required");
        }
        return ResponseEntity.ok(service.listBotContexts());
    }
}
