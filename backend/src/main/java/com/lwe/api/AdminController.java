package com.lwe.api;

import com.lwe.api.dto.*;
import com.lwe.core.domain.SubscriptionPlan;
import com.lwe.core.domain.User;
import com.lwe.core.repository.SubscriptionPlanRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.events.EventArchiveJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepo;
    private final WorldRepository worldRepo;
    private final SubscriptionPlanRepository planRepo;
    private final EventArchiveJob archiveJob;

    public AdminController(UserRepository userRepo, WorldRepository worldRepo,
                           SubscriptionPlanRepository planRepo, EventArchiveJob archiveJob) {
        this.userRepo = userRepo;
        this.worldRepo = worldRepo;
        this.planRepo = planRepo;
        this.archiveJob = archiveJob;
    }

    @PostMapping("/events/archive")
    public ResponseEntity<ApiResponse> triggerArchive() {
        archiveJob.archiveOldEvents();
        return ResponseEntity.ok(new ApiResponse("Event archiving triggered"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserInfoResponse>> listUsers() {
        var users = userRepo.findAll().stream()
            .map(u -> new UserInfoResponse(u.getId(), u.getEmail(), u.getUsername(), u.getRole(), u.getLocale()))
            .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserInfoResponse> getUser(@PathVariable UUID id) {
        var user = userRepo.findById(id).orElse(null);
        if (user == null) {
            log.warn("AdminController: user not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserInfoResponse(
            user.getId(), user.getEmail(), user.getUsername(), user.getRole(), user.getLocale()));
    }

    @GetMapping("/users/{id}/plan")
    public ResponseEntity<?> getUserPlan(@PathVariable UUID id) {
        var user = userRepo.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        if (user.getPlanId() == null)
            return ResponseEntity.ok(new FreePlanResponse("FREE", "unlimited"));

        var plan = planRepo.findById(user.getPlanId()).orElse(null);
        if (plan == null)
            return ResponseEntity.ok(new FreePlanResponse("UNKNOWN", "unlimited"));

        return ResponseEntity.ok(new PlanInfoResponse(plan.getId(), plan.getName(),
            plan.getMaxWorlds(), plan.getMaxMembersPerWorld(), plan.isAiModeAllowed(),
            user.getPlanExpiresAt() != null ? user.getPlanExpiresAt().toString() : null));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable UUID id, @RequestBody RoleRequest req) {
        var user = userRepo.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        if (!List.of("USER", "ADMIN", "BOT").contains(req.role()))
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role"));
        user.setRole(req.role());
        userRepo.save(user);
        return ResponseEntity.ok(new UpdatedResponse(true, null));
    }

    @PutMapping("/users/{id}/plan")
    public ResponseEntity<?> assignPlan(@PathVariable UUID id, @RequestBody PlanAssignment req) {
        var user = userRepo.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        var plan = planRepo.findById(req.planId()).orElse(null);
        if (plan == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Plan not found"));
        user.setPlanId(plan.getId());
        userRepo.save(user);
        return ResponseEntity.ok(new UpdatedResponse(true, null));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminWorldStatsResponse> getStats() {
        var totalWorlds = worldRepo.count();
        var activeWorlds = worldRepo.findByActiveTrue().size();
        return ResponseEntity.ok(new AdminWorldStatsResponse(totalWorlds, activeWorlds));
    }

    public record RoleRequest(String role) {}
    public record PlanAssignment(UUID planId) {}
    public record FreePlanResponse(String plan, String limits) {}
}
