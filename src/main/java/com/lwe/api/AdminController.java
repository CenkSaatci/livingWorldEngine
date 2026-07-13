package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin-Endpunkte (Rolle {@code ADMIN}). Zugriff via {@code /api/v1/admin/*}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepo;
    private final WorldRepository worldRepo;

    public AdminController(UserRepository userRepo, WorldRepository worldRepo) {
        this.userRepo = userRepo;
        this.worldRepo = worldRepo;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        var users = userRepo.findAll().stream()
            .map(u -> Map.of("id", u.getId(), "email", u.getEmail(),
                "username", u.getUsername(), "role", u.getRole(),
                "locale", u.getLocale()))
            .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable UUID id) {
        var user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        return ResponseEntity.ok(Map.of(
            "id", user.getId(), "email", user.getEmail(),
            "username", user.getUsername(), "role", user.getRole(),
            "locale", user.getLocale()
        ));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        var user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        var newRole = body.get("role");
        if (newRole != null) {
            try {
                var f = User.class.getDeclaredField("role");
                f.setAccessible(true);
                f.set(user, newRole);
            } catch (Exception ignored) {}
        }
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @GetMapping("/worlds/stats")
    public ResponseEntity<?> worldStats() {
        var totalWorlds = worldRepo.count();
        var activeWorlds = worldRepo.findByActiveTrue().size();
        return ResponseEntity.ok(Map.of(
            "total_worlds", totalWorlds,
            "active_worlds", activeWorlds
        ));
    }
}
